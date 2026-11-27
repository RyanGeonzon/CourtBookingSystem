import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main window after sign in. A sidebar on the left and the selected screen on the right.
 * The Customer and the Owner see different menus.
 */
public class Dashboard extends JFrame {
    private final DataStore ds = DataStore.get();
    private final User user = ds.getCurrentUser();

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JComponent> pages = new HashMap<>();
    private final Map<String, String> titles = new HashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();

    private final JLabel pageTitle = Theme.label(" ", Theme.TITLE, Theme.TEXT);
    private final Theme.Avatar avatar = new Theme.Avatar("?", 38);
    private final JLabel userName = Theme.label(" ", Theme.BOLD, Color.WHITE);
    private final JLabel userRole = Theme.label(" ", Theme.SMALL, new Color(0xBDB3AA));

    public Dashboard() {
        super("Courtside Booking");
        Image logo = Theme.loadImage("logo.jpg");
        if (logo != null) setIconImage(logo);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildMain(), BorderLayout.CENTER);
        registerPages();
        refreshUser();

        setSize(1180, 740);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        navigate("home");
    }

    // ---------- Layout ----------
    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(Theme.SIDEBAR);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(24, 16, 20, 16));
        side.setPreferredSize(new Dimension(236, 0));

        JLabel brand = Theme.label("Courtside", new Font("SansSerif", Font.BOLD, 24), Theme.ACCENT);
        JLabel tag = Theme.label("Court Booking System", Theme.SMALL, new Color(0xBDB3AA));
        brand.setAlignmentX(LEFT_ALIGNMENT);
        tag.setAlignmentX(LEFT_ALIGNMENT);
        brand.setBorder(new EmptyBorder(0, 8, 0, 0));
        tag.setBorder(new EmptyBorder(0, 8, 0, 0));
        side.add(brand);
        side.add(tag);
        side.add(Box.createVerticalStrut(28));

        addNav(side, "home", "Home");
        if (user.isOwner()) {
            addNav(side, "manage", "Booking Records");
            addNav(side, "schedule", "Court Schedule");
            addNav(side, "customers", "Customers");
        } else {
            addNav(side, "book", "Book a Court");
            addNav(side, "bookings", "My Bookings");
        }
        addNav(side, "profile", "Profile");

        side.add(Box.createVerticalGlue());

        JPanel chip = new JPanel(new BorderLayout(10, 0));
        chip.setOpaque(false);
        chip.setAlignmentX(LEFT_ALIGNMENT);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        chip.setBorder(new EmptyBorder(0, 8, 0, 0));
        JPanel names = new JPanel(new GridLayout(2, 1));
        names.setOpaque(false);
        names.add(userName);
        names.add(userRole);
        chip.add(avatar, BorderLayout.WEST);
        chip.add(names, BorderLayout.CENTER);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { navigate("profile"); }
        });
        side.add(chip);
        side.add(Box.createVerticalStrut(14));

        NavButton signOut = new NavButton("Sign out");
        signOut.setAlignmentX(LEFT_ALIGNMENT);
        signOut.addActionListener(e -> signOut());
        side.add(signOut);
        return side;
    }

    private void addNav(JPanel side, String key, String text) {
        NavButton b = new NavButton(text);
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.addActionListener(e -> navigate(key));
        navButtons.put(key, b);
        titles.put(key, text);
        side.add(b);
        side.add(Box.createVerticalStrut(4));
    }

    private JComponent buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(16, 28, 16, 28)));
        header.add(pageTitle, BorderLayout.WEST);
        header.add(Theme.label(LocalDate.now().format(Theme.DATE_FMT), Theme.BODY, Theme.MUTED), BorderLayout.EAST);
        main.add(header, BorderLayout.NORTH);

        content.setBackground(Theme.BG);
        main.add(content, BorderLayout.CENTER);
        return main;
    }

    private void registerPages() {
        addPage("home", new HomePanel(this));
        if (user.isOwner()) {
            addPage("manage", new ManageBookingsPanel(this));
            addPage("schedule", new CourtSchedulePanel(this));
            addPage("customers", new CustomersPanel(this));
        } else {
            addPage("book", new BookCourtPanel(this));
            addPage("bookings", new MyBookingsPanel(this));
        }
        addPage("profile", new ProfilePanel(this));
        titles.put("home", "Home");
        titles.put("profile", "My Profile");
    }

    private void addPage(String key, JComponent page) {
        pages.put(key, page);
        content.add(page, key);
    }

    // ---------- Actions ----------
    /** Show a screen by its key. Screens reload their data each time. */
    public void navigate(String key) {
        JComponent page = pages.get(key);
        if (page == null) return;
        if (page instanceof Refreshable) ((Refreshable) page).refresh();
        cards.show(content, key);
        pageTitle.setText(titles.getOrDefault(key, key));
        for (Map.Entry<String, NavButton> e : navButtons.entrySet()) {
            e.getValue().setSelected(e.getKey().equals(key));
        }
    }

    /** Call after the profile changes so the sidebar shows the new name. */
    public void refreshUser() {
        avatar.setPerson(user.getName());
        userName.setText(user.getName());
        userRole.setText(user.getRole().toString());
    }

    private void signOut() {
        int ok = JOptionPane.showConfirmDialog(this, "Sign out of Courtside?", "Sign out",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        ds.setCurrentUser(null);
        dispose();
        new LoginFrame().setVisible(true);
    }

    // ---------- Sidebar button ----------
    private static class NavButton extends JButton {
        private boolean active;
        private boolean over;

        NavButton(String text) {
            super(text);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setPreferredSize(new Dimension(0, 42));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { over = false; repaint(); }
            });
        }

        @Override
        public void setSelected(boolean b) {
            active = b;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (over) {
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
