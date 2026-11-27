import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Home screen. Shows a greeting, a few numbers, upcoming bookings and quick actions for the signed-in role. */
public class HomePanel extends JPanel implements Refreshable {
    private final Dashboard dash;
    private final DataStore ds = DataStore.get();
    private final JPanel body = new JPanel();

    public HomePanel(Dashboard dash) {
        this.dash = dash;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(24, 28, 24, 28));
        add(Theme.pageScroll(body), BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        User user = ds.getCurrentUser();
        if (user == null) return;
        body.removeAll();

        JComponent hero = new Hero(user);
        hero.setAlignmentX(LEFT_ALIGNMENT);
        body.add(hero);
        body.add(Box.createVerticalStrut(18));

        JPanel stats = new JPanel(new GridLayout(1, 4, 16, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        if (user.isOwner()) buildOwnerStats(stats); else buildCustomerStats(stats, user);
        body.add(stats);
        body.add(Box.createVerticalStrut(18));

        JPanel lower = new JPanel(new BorderLayout(18, 0));
        lower.setOpaque(false);
        lower.setAlignmentX(LEFT_ALIGNMENT);
        lower.add(buildUpcomingCard(user), BorderLayout.CENTER);
        lower.add(buildActionsCard(user), BorderLayout.EAST);
        body.add(lower);

        body.revalidate();
        body.repaint();
    }

    // ---------- Stat cards ----------
    private void buildCustomerStats(JPanel stats, User user) {
        List<Booking> mine = ds.bookingsOf(user.getEmail());
        int upcoming = 0, toPay = 0, total = 0;
        double paid = 0;
        for (Booking b : mine) {
            if (b.isUpcoming()) upcoming++;
            if (b.canPay()) toPay++;
            if (b.isActive()) total++;
            if (b.isPaid()) paid += b.getFee();
        }
        stats.add(statCard(String.valueOf(upcoming), "Upcoming bookings", Theme.TEXT));
        stats.add(statCard(String.valueOf(toPay), "Waiting for your payment", toPay > 0 ? Theme.WARNING : Theme.TEXT));
        stats.add(statCard(String.valueOf(total), "Total bookings", Theme.TEXT));
        stats.add(statCard(Theme.money(paid), "Paid so far", Theme.SUCCESS));
    }

    private void buildOwnerStats(JPanel stats) {
        int toVerify = 0, today = 0;
        LocalDate now = LocalDate.now();
        for (Booking b : ds.allBookings()) {
            if (b.isActive() && b.getPaymentStatus() == Booking.PaymentStatus.PENDING_VERIFICATION) toVerify++;
            if (b.isActive() && b.getDate().equals(now)) today++;
        }
        stats.add(statCard(String.valueOf(toVerify), "Payments to verify", toVerify > 0 ? Theme.WARNING : Theme.TEXT));
        stats.add(statCard(String.valueOf(today), "Bookings today", Theme.TEXT));
        stats.add(statCard(String.valueOf(ds.customers().size()), "Registered customers", Theme.TEXT));
        stats.add(statCard(Theme.money(ds.revenue()), "Verified revenue", Theme.SUCCESS));
    }

    private JPanel statCard(String value, String caption, Color color) {
        Theme.Card c = new Theme.Card(new BorderLayout(0, 4));
        c.add(Theme.label(value, new Font("SansSerif", Font.BOLD, 26), color), BorderLayout.CENTER);
        c.add(Theme.muted(caption), BorderLayout.SOUTH);
        return c;
    }

    // ---------- Upcoming bookings ----------
    private JPanel buildUpcomingCard(User user) {
        boolean owner = user.isOwner();
        List<Booking> list = new ArrayList<>();
        List<Booking> source = owner ? ds.allBookings() : ds.bookingsOf(user.getEmail());
        for (Booking b : source) if (b.isUpcoming()) list.add(b);
        list.sort(Comparator.comparing(Booking::getDate).thenComparing(Booking::getStartHour));

        String[] cols = owner
                ? new String[]{"ID", "Customer", "Court", "Date", "Time", "Payment"}
                : new String[]{"ID", "Court", "Date", "Time", "Booking", "Payment"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        int shown = 0;
        for (Booking b : list) {
            if (shown++ == 6) break;
            if (owner) {
                User u = ds.findUser(b.getCustomerEmail());
                model.addRow(new Object[]{b.getId(), u == null ? b.getCustomerEmail() : u.getName(), b.getCourt(),
                        b.getDate().format(Theme.DAY_FMT), b.getTimeRange(), b.getPaymentStatus().toString()});
            } else {
                model.addRow(new Object[]{b.getId(), b.getCourt(), b.getDate().format(Theme.DAY_FMT),
                        b.getTimeRange(), b.getStatus().toString(), b.getPaymentStatus().toString()});
            }
        }
        JTable table = new JTable(model);
        Theme.styleTable(table);
        table.setEnabled(false);
        table.getColumnModel().getColumn(5).setCellRenderer(new Theme.StatusRenderer());
        if (!owner) table.getColumnModel().getColumn(4).setCellRenderer(new Theme.StatusRenderer());
        int[] widths = owner ? new int[]{70, 130, 70, 110, 170, 150} : new int[]{70, 70, 110, 170, 90, 150};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.add(Theme.label(owner ? "Upcoming bookings (all customers)" : "Your upcoming bookings",
                Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        if (list.isEmpty()) {
            JLabel empty = Theme.muted(owner ? "No upcoming bookings yet."
                    : "You have no upcoming bookings. Book a court to get started.");
            empty.setBorder(new EmptyBorder(40, 0, 40, 0));
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(empty, BorderLayout.CENTER);
        } else {
            JScrollPane sp = Theme.tableScroll(table);
            sp.setPreferredSize(new Dimension(100, 36 + 35 * Math.min(list.size(), 6) + 6));
            card.add(sp, BorderLayout.CENTER);
        }
        return card;
    }

    // ---------- Quick actions ----------
    private JPanel buildActionsCard(User user) {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 12));
        card.add(Theme.label("Quick actions", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);

        JPanel col = new JPanel(new GridLayout(0, 1, 0, 10));
        col.setOpaque(false);
        if (user.isOwner()) {
            col.add(action("Verify payments", "manage", true));
            col.add(action("Court schedule", "schedule", false));
            col.add(action("Customers", "customers", false));
        } else {
            col.add(action("Book a court", "book", true));
            col.add(action("My bookings", "bookings", false));
        }
        col.add(action("My profile", "profile", false));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(col, BorderLayout.NORTH);
        card.add(top, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(250, card.getPreferredSize().height));
        return card;
    }

    private JButton action(String text, String page, boolean primary) {
        JButton b = primary ? Theme.primary(text) : Theme.secondary(text);
        b.setPreferredSize(new Dimension(0, 42));
        b.addActionListener(e -> dash.navigate(page));
        return b;
    }

    // ---------- Hero banner ----------
    private static class Hero extends JPanel {
        Hero(User user) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(28, 32, 28, 32));
            setPreferredSize(new Dimension(0, 150));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

            int h = LocalTime.now().getHour();
            String greet = h < 12 ? "Good morning" : (h < 18 ? "Good afternoon" : "Good evening");
            String first = user.getName().trim().split("\\s+")[0];
            JLabel title = Theme.label(greet + ", " + first, new Font("SansSerif", Font.BOLD, 28), Color.WHITE);
            JLabel sub = Theme.label(user.isOwner()
                            ? "Here is what is happening at the court today."
                            : "Your court is waiting. Pick a time and book it in a minute.",
                    new Font("SansSerif", Font.PLAIN, 15), new Color(0xFFF3EA));
            add(Box.createVerticalGlue());
            add(title);
            add(Box.createVerticalStrut(6));
            add(sub);
            add(Box.createVerticalGlue());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 18, 18);
            g2.setClip(clip);
            g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT, w, h, Theme.ACCENT_DARK));
            g2.fillRect(0, 0, w, h);

            // Simple basketball court line art on the right
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(3f));
            int midX = w - 250;
            g2.drawLine(midX, -5, midX, h + 5);
            g2.drawOval(midX - 52, h / 2 - 52, 104, 104);
            g2.drawRect(w - 95, h / 2 - 50, 100, 100);
            g2.drawArc(w - 95 - 50, h / 2 - 50, 100, 100, 90, 180);
            g2.drawArc(w - 95 - 100, h / 2 - 100, 200, 200, 120, 120);
            g2.dispose();
        }
    }
}
