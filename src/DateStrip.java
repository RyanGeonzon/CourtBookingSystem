import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

/** A grid of day buttons (today + the next N days). Used to pick a booking date. */
public class DateStrip extends JPanel {
    private LocalDate selected;

    public DateStrip(int days, Consumer<LocalDate> onChange) {
        super(new GridLayout(0, 7, 6, 6));
        setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < days; i++) {
            final LocalDate d = today.plusDays(i);
            String html = "<html><center>" + d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + "<br><b style='font-size:14px'>" + d.getDayOfMonth() + "</b><br>"
                    + d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + "</center></html>";
            DayButton b = new DayButton(html);
            group.add(b);
            add(b);
            if (i == 0) {
                b.setSelected(true);
                b.setForeground(Color.WHITE);
                selected = d;
            }
            b.addItemListener(e -> {
                b.setForeground(b.isSelected() ? Color.WHITE : Theme.TEXT);
                if (b.isSelected()) {
                    selected = d;
                    onChange.accept(d);
                }
            });
        }
    }

    public LocalDate getSelectedDate() { return selected; }

    private static class DayButton extends JToggleButton {
        DayButton(String html) {
            super(html);
            setFont(Theme.SMALL);
            setForeground(Theme.TEXT);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setPreferredSize(new Dimension(64, 62));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
