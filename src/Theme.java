import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/** Shared colors, fonts and small UI building blocks so every screen looks the same. */
public final class Theme {
    private Theme() { }

    // ---------- Colors ----------
    public static final Color ACCENT = new Color(0xE27C3C);
    public static final Color ACCENT_DARK = new Color(0xC4642A);
    public static final Color ACCENT_LIGHT = new Color(0xFCEBDD);
    public static final Color BG = new Color(0xF6F4F1);
    public static final Color SIDEBAR = new Color(0x2B2420);
    public static final Color TEXT = new Color(0x2B2420);
    public static final Color MUTED = new Color(0x7A716A);
    public static final Color BORDER = new Color(0xE3DDD6);
    public static final Color ERROR = new Color(0xB3261E);
    public static final Color SUCCESS = new Color(0x2E7D32);
    public static final Color WARNING = new Color(0xB26A00);

    // ---------- Fonts ----------
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 24);
    public static final Font HEADING = new Font("SansSerif", Font.BOLD, 16);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 12);

    // ---------- Formatting ----------
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");
    public static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
    public static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d");

    /** Philippine peso. \u20B1 is the peso sign. */
    public static String money(double v) {
        return "\u20B1" + MONEY.format(v);
    }

    /** 14 becomes "2:00 PM". 24 becomes "12:00 AM". */
    public static String hour(int h) {
        h = ((h % 24) + 24) % 24;
        int h12 = (h % 12 == 0) ? 12 : h % 12;
        return h12 + ":00 " + (h < 12 ? "AM" : "PM");
    }

    public static String range(int start, int hours) {
        return hour(start) + " - " + hour(start + hours);
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    public static Image loadImage(String path) {
        try {
            URL url = Theme.class.getResource(path);
            return url == null ? null : ImageIO.read(url);
        } catch (IOException e) {
            return null;
        }
    }

    // ---------- Labels ----------
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JLabel muted(String text) {
        return label(text, SMALL, MUTED);
    }

    /** A label sitting above an input, used to build forms. */
    public static JPanel labeled(String text, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(label(text, BOLD, TEXT), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ---------- Inputs ----------
    public static void styleField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setPreferredSize(new Dimension(0, 38));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
    }

    public static JComboBox<Integer> hourCombo(int from, int to) {
        JComboBox<Integer> box = new JComboBox<>();
        for (int h = from; h <= to; h++) box.addItem(h);
        box.setFont(BODY);
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value instanceof Integer) setText(hour((Integer) value));
                return this;
            }
        });
        return box;
    }

    // ---------- Buttons ----------
    public static class RoundButton extends JButton {
        private final Color base, hover, outline;
        private boolean over;

        public RoundButton(String text, Color base, Color hover, Color fg, Color outline) {
            super(text);
            this.base = base;
            this.hover = hover;
            this.outline = outline;
            setForeground(fg);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(9, 18, 9, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { over = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled()) {
                g2.setColor(new Color(0xEFEBE6));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else {
                g2.setColor(over ? hover : base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (outline != null) {
                    g2.setColor(outline);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static RoundButton primary(String text) {
        return new RoundButton(text, ACCENT, ACCENT_DARK, Color.WHITE, null);
    }

    public static RoundButton secondary(String text) {
        return new RoundButton(text, Color.WHITE, ACCENT_LIGHT, ACCENT_DARK, new Color(0xE8B796));
    }

    public static RoundButton danger(String text) {
        return new RoundButton(text, Color.WHITE, new Color(0xFBE4E2), ERROR, new Color(0xE3A9A4));
    }

    // ---------- Cards, avatar ----------
    public static class Card extends JPanel {
        public Card(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(18, 20, 18, 20));
        }

        public Card() {
            this(new BorderLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class Avatar extends JComponent {
        private String text;
        private final int size;

        public Avatar(String name, int size) {
            this.size = size;
            this.text = initials(name);
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        public void setPerson(String name) {
            text = initials(name);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(ACCENT);
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, (int) (size * 0.38))));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (size - fm.stringWidth(text)) / 2,
                    (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ---------- Tables ----------
    public static void styleTable(JTable t) {
        t.setRowHeight(34);
        t.setFont(BODY);
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(0xEFEAE4));
        t.setSelectionBackground(ACCENT_LIGHT);
        t.setSelectionForeground(TEXT);
        t.setFillsViewportHeight(true);
        t.setIntercellSpacing(new Dimension(0, 1));
        JTableHeader h = t.getTableHeader();
        h.setFont(BOLD);
        h.setBackground(new Color(0xF3EFEA));
        h.setForeground(MUTED);
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 36));
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBorder(new EmptyBorder(0, 10, 0, 10));
        t.setDefaultRenderer(Object.class, r);
    }

    public static JScrollPane tableScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    public static Color statusColor(String s) {
        switch (s) {
            case "Confirmed": case "Completed": case "Verified": return SUCCESS;
            case "Pending": case "Awaiting verification": return WARNING;
            case "Cancelled": case "Rejected": return ERROR;
            default: return MUTED;
        }
    }

    /** Colors the text of a Status or Payment column. */
    public static class StatusRenderer extends DefaultTableCellRenderer {
        public StatusRenderer() {
            setBorder(new EmptyBorder(0, 10, 0, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setFont(BOLD);
            setForeground(statusColor(String.valueOf(value)));
            return this;
        }
    }

    // ---------- Scrolling page content ----------
    private static class TrackingPanel extends JPanel implements Scrollable {
        TrackingPanel(LayoutManager lm) { super(lm); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 96; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Wraps page content so it scrolls vertically on small windows but always fills the width. */
    public static JScrollPane pageScroll(JComponent content) {
        JPanel holder = new TrackingPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(content, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(holder, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }
}
