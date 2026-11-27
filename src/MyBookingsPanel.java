import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Customer screen: View Booking Details, Make Payment and cancel a booking. */
public class MyBookingsPanel extends JPanel implements Refreshable {
    private static final String[] COLS = {"ID", "Court", "Date", "Time", "Fee", "Booking", "Payment"};

    private final DataStore ds = DataStore.get();
    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> filter =
            new JComboBox<>(new String[]{"All", "Pending", "Confirmed", "Completed", "Cancelled"});
    private final List<Booking> shown = new ArrayList<>();
    private final JLabel details = new JLabel(" ");
    private final JButton payBtn = Theme.primary("Pay with GCash");
    private final JButton cancelBtn = Theme.danger("Cancel booking");

    public MyBookingsPanel(Dashboard dash) {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(5).setCellRenderer(new Theme.StatusRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new Theme.StatusRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(170);
        table.getColumnModel().getColumn(6).setPreferredWidth(150);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateDetails(); });

        filter.setFont(Theme.BODY);
        filter.addActionListener(e -> refresh());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.muted("Select a booking to see its details or to pay."), BorderLayout.WEST);
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        f.setOpaque(false);
        f.add(Theme.muted("Show"));
        f.add(filter);
        top.add(f, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        add(Theme.tableScroll(table), BorderLayout.CENTER);

        Theme.Card bottom = new Theme.Card(new BorderLayout(0, 12));
        details.setFont(Theme.BODY);
        details.setForeground(Theme.TEXT);
        bottom.add(details, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancelBtn);
        buttons.add(payBtn);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        payBtn.addActionListener(e -> {
            Booking b = selected();
            if (b == null) return;
            new PaymentDialog(SwingUtilities.getWindowAncestor(this), b).setVisible(true);
            refresh();
        });
        cancelBtn.addActionListener(e -> {
            Booking b = selected();
            if (b == null) return;
            int ok = JOptionPane.showConfirmDialog(this,
                    "Cancel booking " + b.getId() + "? The time slot will open up for other customers.",
                    "Cancel booking", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                ds.cancelBooking(b);
                refresh();
            }
        });
        refresh();
    }

    private Booking selected() {
        int r = table.getSelectedRow();
        return (r >= 0 && r < shown.size()) ? shown.get(r) : null;
    }

    @Override
    public void refresh() {
        Booking keep = selected();
        String want = (String) filter.getSelectedItem();
        shown.clear();
        model.setRowCount(0);
        for (Booking b : ds.bookingsOf(ds.getCurrentUser().getEmail())) {
            if (!"All".equals(want) && !b.getStatus().toString().equals(want)) continue;
            shown.add(b);
            model.addRow(new Object[]{b.getId(), b.getCourt(), b.getDate().format(Theme.DAY_FMT),
                    b.getTimeRange(), Theme.money(b.getFee()), b.getStatus().toString(),
                    b.getPaymentStatus().toString()});
        }
        int idx = keep == null ? -1 : shown.indexOf(keep);
        if (idx >= 0) table.setRowSelectionInterval(idx, idx);
        updateDetails();
    }

    private void updateDetails() {
        Booking b = selected();
        payBtn.setEnabled(b != null && b.canPay());
        cancelBtn.setEnabled(b != null && b.getStatus() == Booking.Status.PENDING);
        if (b == null) {
            details.setText(shown.isEmpty() ? "No bookings to show yet. Use \"Book a Court\" to make one." : "No booking selected.");
            return;
        }
        String hint;
        switch (b.getPaymentStatus()) {
            case UNPAID: hint = b.getStatus() == Booking.Status.CANCELLED
                    ? "This booking was cancelled." : "Pay through GCash to get this booking confirmed."; break;
            case PENDING_VERIFICATION: hint = "Payment submitted. Waiting for the Owner to verify it."; break;
            case REJECTED: hint = "The Owner could not verify that payment. Check the reference number and pay again."; break;
            default: hint = b.getStatus() == Booking.Status.CANCELLED ? "This booking was cancelled."
                    : "Payment verified. See you on the court!"; break;
        }
        String ref = b.getRefNumber().isEmpty() ? "" : "  |  GCash ref " + b.getRefNumber();
        details.setText("<html><b>" + b.getId() + "</b>  |  " + b.getCourt() + "  |  "
                + b.getDate().format(Theme.DATE_FMT) + "  |  " + b.getTimeRange() + "<br>"
                + Theme.money(b.getFee()) + ref + "<br><span style='color:#7A716A'>" + hint + "</span></html>");
    }
}
