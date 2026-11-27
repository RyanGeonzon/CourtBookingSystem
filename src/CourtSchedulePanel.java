import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;

/** Owner screen: Manage Court Schedule. Set opening hours and rates, and block or reopen time slots. */
public class CourtSchedulePanel extends JPanel implements Refreshable {
    private final DataStore ds = DataStore.get();

    private final JComboBox<Integer> openBox = Theme.hourCombo(0, 23);
    private final JComboBox<Integer> closeBox = Theme.hourCombo(1, 24);
    private final JComboBox<Integer> peakBox = Theme.hourCombo(0, 23);
    private final JSpinner regularRate = new JSpinner(new SpinnerNumberModel(300.0, 0.0, 100000.0, 50.0));
    private final JSpinner peakRate = new JSpinner(new SpinnerNumberModel(400.0, 0.0, 100000.0, 50.0));
    private final JLabel settingsMsg = new JLabel(" ");

    private final JComboBox<String> courtBox = new JComboBox<>(DataStore.get().getCourts().toArray(new String[0]));
    private final DateStrip strip;
    private final SlotGrid grid;

    public CourtSchedulePanel(Dashboard dash) {
        setBackground(Theme.BG);
        setLayout(new BorderLayout());

        strip = new DateStrip(14, d -> showGrid());
        grid = new SlotGrid((hour, state) -> onSlotClicked(hour, state), true);
        courtBox.setFont(Theme.BODY);
        courtBox.setPreferredSize(new Dimension(260, 34));
        courtBox.setMaximumSize(new Dimension(260, 34));
        courtBox.addActionListener(e -> showGrid());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(24, 28, 24, 28));
        JComponent settings = buildSettingsCard();
        JComponent availability = buildAvailabilityCard();
        settings.setAlignmentX(LEFT_ALIGNMENT);
        availability.setAlignmentX(LEFT_ALIGNMENT);
        body.add(settings);
        body.add(Box.createVerticalStrut(18));
        body.add(availability);
        add(Theme.pageScroll(body), BorderLayout.CENTER);

        refresh();
    }

    private JComponent buildSettingsCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.add(Theme.label("Operating hours and rates", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);

        JPanel grid5 = new JPanel(new GridLayout(1, 5, 14, 0));
        grid5.setOpaque(false);
        regularRate.setFont(Theme.BODY);
        peakRate.setFont(Theme.BODY);
        grid5.add(Theme.labeled("Opens at", openBox));
        grid5.add(Theme.labeled("Closes at", closeBox));
        grid5.add(Theme.labeled("Regular rate per hour", regularRate));
        grid5.add(Theme.labeled("Peak hours start", peakBox));
        grid5.add(Theme.labeled("Peak rate per hour", peakRate));
        card.add(grid5, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        settingsMsg.setFont(Theme.SMALL);
        south.add(settingsMsg, BorderLayout.CENTER);
        JButton save = Theme.primary("Save settings");
        save.addActionListener(e -> onSaveSettings());
        south.add(save, BorderLayout.EAST);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildAvailabilityCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel h = Theme.label("Court availability", Theme.HEADING, Theme.TEXT);
        h.setAlignmentX(LEFT_ALIGNMENT);
        JLabel hint = Theme.muted("Click an available slot to block it (maintenance, private event). "
                + "Click a blocked slot to open it again. Click a booked slot to see who booked it.");
        hint.setAlignmentX(LEFT_ALIGNMENT);
        courtBox.setAlignmentX(LEFT_ALIGNMENT);
        strip.setAlignmentX(LEFT_ALIGNMENT);
        JPanel legend = SlotGrid.legend();
        legend.setAlignmentX(LEFT_ALIGNMENT);
        top.add(h);
        top.add(Box.createVerticalStrut(4));
        top.add(hint);
        top.add(Box.createVerticalStrut(14));
        top.add(courtBox);
        top.add(Box.createVerticalStrut(12));
        top.add(strip);
        top.add(Box.createVerticalStrut(14));
        top.add(legend);

        card.add(top, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh() {
        openBox.setSelectedItem(ds.getOpenHour());
        closeBox.setSelectedItem(ds.getCloseHour());
        peakBox.setSelectedItem(ds.getPeakStart());
        regularRate.setValue(ds.getRegularRate());
        peakRate.setValue(ds.getPeakRate());
        settingsMsg.setText(" ");
        showGrid();
    }

    private void showGrid() {
        grid.show((String) courtBox.getSelectedItem(), strip.getSelectedDate(), -1, 0);
    }

    private void onSaveSettings() {
        String error = ds.updateSettings((Integer) openBox.getSelectedItem(), (Integer) closeBox.getSelectedItem(),
                (Integer) peakBox.getSelectedItem(), ((Number) regularRate.getValue()).doubleValue(),
                ((Number) peakRate.getValue()).doubleValue());
        if (error != null) {
            settingsMsg.setForeground(Theme.ERROR);
            settingsMsg.setText(error);
            return;
        }
        settingsMsg.setForeground(Theme.SUCCESS);
        settingsMsg.setText("Saved. New bookings use these hours and rates.");
        showGrid();
    }

    private void onSlotClicked(int hour, DataStore.SlotState state) {
        String court = (String) courtBox.getSelectedItem();
        LocalDate date = strip.getSelectedDate();
        switch (state) {
            case AVAILABLE:
            case BLOCKED:
                ds.toggleBlock(court, date, hour);
                showGrid();
                break;
            case BOOKED:
                Booking b = ds.bookingAt(court, date, hour);
                if (b != null) {
                    User u = ds.findUser(b.getCustomerEmail());
                    JOptionPane.showMessageDialog(this,
                            b.getId() + "  |  " + b.getTimeRange() + "\nBooked by: "
                                    + (u == null ? b.getCustomerEmail() : u.getName())
                                    + "\nBooking: " + b.getStatus() + "\nPayment: " + b.getPaymentStatus(),
                            "Booked slot", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            default:
                break;
        }
    }
}
