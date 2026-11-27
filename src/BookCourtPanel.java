import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/** Customer screen: Check Available Time Slots, Calculate Booking Fee and Book Basketball Court. */
public class BookCourtPanel extends JPanel implements Refreshable {
    private final Dashboard dash;
    private final DataStore ds = DataStore.get();

    private final JComboBox<String> courtBox = new JComboBox<>(DataStore.get().getCourts().toArray(new String[0]));
    private final DateStrip strip;
    private final SlotGrid grid;
    private final JSpinner duration = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));

    private final JLabel sumCourt = value("-");
    private final JLabel sumDate = value("-");
    private final JLabel sumTime = value("-");
    private final JLabel breakdown = new JLabel(" ");
    private final JLabel total = Theme.label("-", new Font("SansSerif", Font.BOLD, 28), Theme.ACCENT_DARK);
    private final JLabel message = new JLabel(" ");
    private final JButton confirm = Theme.primary("Confirm booking");

    private int selectedHour = -1;

    public BookCourtPanel(Dashboard dash) {
        this.dash = dash;
        setBackground(Theme.BG);
        setLayout(new BorderLayout(20, 0));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        strip = new DateStrip(14, d -> onContextChanged());
        grid = new SlotGrid((hour, state) -> onSlotClicked(hour, state), false);

        courtBox.setFont(Theme.BODY);
        courtBox.setPreferredSize(new Dimension(260, 34));
        courtBox.setMaximumSize(new Dimension(260, 34));
        courtBox.addActionListener(e -> onContextChanged());
        duration.addChangeListener(e -> updateSummary());

        add(Theme.pageScroll(buildPickerCard()), BorderLayout.CENTER);

        JPanel east = new JPanel(new BorderLayout());
        east.setOpaque(false);
        east.setPreferredSize(new Dimension(320, 0));
        east.add(buildSummaryCard(), BorderLayout.NORTH);
        add(east, BorderLayout.EAST);

        updateSummary();
    }

    private JLabel value(String text) {
        return Theme.label(text, Theme.BOLD, Theme.TEXT);
    }

    private JLabel step(String text) {
        JLabel l = Theme.label(text, Theme.HEADING, Theme.TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPanel buildPickerCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(step("1.  Choose a court"));
        top.add(Box.createVerticalStrut(8));
        courtBox.setAlignmentX(LEFT_ALIGNMENT);
        top.add(courtBox);
        top.add(Box.createVerticalStrut(20));
        top.add(step("2.  Pick a date"));
        top.add(Box.createVerticalStrut(8));
        strip.setAlignmentX(LEFT_ALIGNMENT);
        top.add(strip);
        top.add(Box.createVerticalStrut(20));
        top.add(step("3.  Pick a start time"));
        top.add(Box.createVerticalStrut(8));
        JPanel legend = SlotGrid.legend();
        legend.setAlignmentX(LEFT_ALIGNMENT);
        top.add(legend);

        card.add(top, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSummaryCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.add(Theme.label("Booking summary", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(row("Court", sumCourt));
        rows.add(Box.createVerticalStrut(8));
        rows.add(row("Date", sumDate));
        rows.add(Box.createVerticalStrut(8));
        rows.add(row("Time", sumTime));
        rows.add(Box.createVerticalStrut(8));
        duration.setPreferredSize(new Dimension(70, 28));
        JPanel durRow = row("Hours", duration);
        rows.add(durRow);
        rows.add(Box.createVerticalStrut(14));

        breakdown.setFont(Theme.SMALL);
        breakdown.setForeground(Theme.MUTED);
        breakdown.setAlignmentX(LEFT_ALIGNMENT);
        rows.add(breakdown);
        rows.add(Box.createVerticalStrut(12));

        JLabel totalCap = Theme.muted("Total fee");
        totalCap.setAlignmentX(LEFT_ALIGNMENT);
        total.setAlignmentX(LEFT_ALIGNMENT);
        rows.add(totalCap);
        rows.add(total);
        rows.add(Box.createVerticalStrut(8));

        message.setFont(Theme.SMALL);
        message.setAlignmentX(LEFT_ALIGNMENT);
        rows.add(message);
        card.add(rows, BorderLayout.CENTER);

        confirm.setPreferredSize(new Dimension(0, 44));
        confirm.addActionListener(e -> onConfirm());
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(confirm, BorderLayout.NORTH);
        south.add(Theme.muted("<html><div style='width:230px'>New bookings start as Pending until your GCash payment is verified.</div></html>"),
                BorderLayout.SOUTH);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private JPanel row(String key, JComponent val) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        p.add(Theme.label(key, Theme.BODY, Theme.MUTED), BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(val);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ---------- Behaviour ----------
    @Override
    public void refresh() {
        selectedHour = -1;
        updateSummary();
    }

    private void onContextChanged() {
        selectedHour = -1;
        updateSummary();
    }

    private void onSlotClicked(int hour, DataStore.SlotState state) {
        if (state != DataStore.SlotState.AVAILABLE) {
            setMessage("That time slot is not available.", Theme.ERROR);
            return;
        }
        selectedHour = hour;
        updateSummary();
    }

    private void setMessage(String text, Color color) {
        message.setForeground(color);
        message.setText("<html><div style='width:230px'>" + text + "</div></html>");
    }

    private void updateSummary() {
        String court = (String) courtBox.getSelectedItem();
        LocalDate date = strip.getSelectedDate();
        int hours = (Integer) duration.getValue();

        grid.show(court, date, selectedHour, hours);
        sumCourt.setText(court);
        sumDate.setText(date.format(Theme.SHORT_DATE));

        if (selectedHour < 0) {
            sumTime.setText("-");
            breakdown.setText(" ");
            total.setText("-");
            confirm.setEnabled(false);
            setMessage("Pick a start time to see the fee.", Theme.MUTED);
            return;
        }

        sumTime.setText(Theme.range(selectedHour, hours));
        if (!ds.isRangeAvailable(court, date, selectedHour, hours)) {
            breakdown.setText(" ");
            total.setText("-");
            confirm.setEnabled(false);
            setMessage("Not every hour in that range is free. Try fewer hours or another start time.",
                    Theme.ERROR);
            return;
        }

        StringBuilder sb = new StringBuilder("<html><table cellspacing='0' cellpadding='1' width='235'>");
        for (int h = selectedHour; h < selectedHour + hours; h++) {
            boolean peak = ds.getPeakRate() != ds.getRegularRate() && h >= ds.getPeakStart();
            sb.append("<tr><td>").append(Theme.hour(h)).append(peak ? " (peak)" : "")
                    .append("</td><td align='right'>").append(Theme.money(ds.rateFor(h))).append("</td></tr>");
        }
        sb.append("</table></html>");
        breakdown.setText(sb.toString());
        total.setText(Theme.money(ds.computeFee(selectedHour, hours)));
        confirm.setEnabled(true);
        setMessage("This time is available.", Theme.SUCCESS);
    }

    private void onConfirm() {
        String court = (String) courtBox.getSelectedItem();
        LocalDate date = strip.getSelectedDate();
        int hours = (Integer) duration.getValue();

        // The system checks again for an existing active booking before saving.
        Booking b = ds.createBooking(ds.getCurrentUser(), court, date, selectedHour, hours);
        if (b == null) {
            selectedHour = -1;
            updateSummary();
            setMessage("Sorry, that time was just taken. Please pick another slot.", Theme.ERROR);
            return;
        }

        String msg = "Booking " + b.getId() + " was created with Pending status.\n\n"
                + b.getCourt() + "  |  " + b.getDate().format(Theme.DATE_FMT) + "\n"
                + b.getTimeRange() + "\n"
                + "Total: " + Theme.money(b.getFee()) + "\n\n"
                + "The time is reserved for you. Pay through GCash so the Owner can confirm it.";
        int choice = JOptionPane.showOptionDialog(this, msg, "Booking created",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new String[]{"Pay now", "Pay later"}, "Pay now");

        selectedHour = -1;
        if (choice == 0) {
            new PaymentDialog(SwingUtilities.getWindowAncestor(this), b).setVisible(true);
        }
        dash.navigate("bookings");
    }
}
