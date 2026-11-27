import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

/** Hourly time slots for one court on one date. Shared by the booking screen and the Owner's schedule screen. */
public class SlotGrid extends JPanel {
    public interface SlotListener {
        void slotClicked(int hour, DataStore.SlotState state);
    }

    private static final Color BOOKED_BG = new Color(0xF6DAD6);
    private static final Color BLOCKED_BG = new Color(0xE4DFD9);
    private static final Color PAST_BG = new Color(0xF2EFEB);

    private final DataStore ds = DataStore.get();
    private final SlotListener listener;
    private final boolean ownerView;
    private String court;
    private LocalDate date;
    private int selStart = -1;
    private int selHours = 0;

    public SlotGrid(SlotListener listener, boolean ownerView) {
        super(new GridLayout(0, 4, 8, 8));
        this.listener = listener;
        this.ownerView = ownerView;
        setOpaque(false);
    }

    /** Redraw the grid. selStart = -1 means nothing is selected. */
    public void show(String court, LocalDate date, int selStart, int selHours) {
        this.court = court;
        this.date = date;
        this.selStart = selStart;
        this.selHours = selHours;
        removeAll();
        for (int h = ds.getOpenHour(); h < ds.getCloseHour(); h++) add(makeButton(h));
        revalidate();
        repaint();
    }

    private SlotButton makeButton(final int h) {
        final DataStore.SlotState state = ds.slotState(court, date, h);
        boolean selected = selStart >= 0 && h >= selStart && h < selStart + selHours;

        String label;
        Color fill, fg, line;
        if (selected && state == DataStore.SlotState.AVAILABLE) {
            label = "Selected"; fill = Theme.ACCENT; fg = Color.WHITE; line = Theme.ACCENT_DARK;
        } else {
            switch (state) {
                case AVAILABLE: label = "Available"; fill = Color.WHITE; fg = Theme.TEXT; line = new Color(0xCFC7BF); break;
                case BOOKED:    label = "Booked";    fill = BOOKED_BG;   fg = Theme.ERROR; line = new Color(0xE3A9A4); break;
                case BLOCKED:   label = "Blocked";   fill = BLOCKED_BG;  fg = Theme.MUTED; line = new Color(0xCFC7BF); break;
                default:        label = "Past";      fill = PAST_BG;     fg = new Color(0xA39A92); line = PAST_BG; break;
            }
        }

        String html = "<html><center><b>" + Theme.hour(h) + "</b><br><span style='font-size:9px'>"
                + label + "</span></center></html>";
        SlotButton b = new SlotButton(html, fill, fg, line, state == DataStore.SlotState.AVAILABLE && !selected);
        boolean clickable = ownerView ? state != DataStore.SlotState.PAST : state == DataStore.SlotState.AVAILABLE;
        b.setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        if (state == DataStore.SlotState.BOOKED && ownerView) {
            Booking bk = ds.bookingAt(court, date, h);
            User u = bk == null ? null : ds.findUser(bk.getCustomerEmail());
            if (bk != null) b.setToolTipText(bk.getId() + " - " + (u == null ? bk.getCustomerEmail() : u.getName()));
        }
        b.addActionListener(e -> listener.slotClicked(h, state));
        return b;
    }

    // ---------- Legend ----------
    public static JPanel legend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        p.setOpaque(false);
        p.add(legendItem(Color.WHITE, new Color(0xCFC7BF), "Available"));
        p.add(legendItem(Theme.ACCENT, Theme.ACCENT_DARK, "Selected"));
        p.add(legendItem(BOOKED_BG, new Color(0xE3A9A4), "Booked"));
        p.add(legendItem(BLOCKED_BG, new Color(0xCFC7BF), "Blocked"));
        p.add(legendItem(PAST_BG, new Color(0xE3DDD6), "Past"));
        return p;
    }

    private static JPanel legendItem(Color fill, Color line, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);
        JComponent swatch = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, 14, 14, 5, 5);
                g2.setColor(line);
                g2.drawRoundRect(0, 0, 13, 13, 5, 5);
                g2.dispose();
            }
        };
        swatch.setPreferredSize(new Dimension(15, 15));
        item.add(swatch);
        item.add(Theme.muted(text));
        return item;
    }

    // ---------- Button ----------
    private static class SlotButton extends JButton {
        private final Color fill, line;
        private final boolean hoverable;
        private boolean over;

        SlotButton(String html, Color fill, Color fg, Color line, boolean hoverable) {
            super(html);
            this.fill = fill;
            this.line = line;
            this.hoverable = hoverable;
            setForeground(fg);
            setFont(Theme.SMALL);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setPreferredSize(new Dimension(100, 54));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { over = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hoverable && over ? Theme.ACCENT_LIGHT : fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.setColor(hoverable && over ? Theme.ACCENT : line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
