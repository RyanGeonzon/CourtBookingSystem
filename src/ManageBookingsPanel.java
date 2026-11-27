import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Owner screen: Manage Booking Records. Verify payments and update booking statuses. */
public class ManageBookingsPanel extends JPanel implements Refreshable {
    private static final String[] COLS =
            {"ID", "Customer", "Court", "Date", "Time", "Fee", "Booking", "Payment", "GCash ref"};

    private final DataStore ds = DataStore.get();
    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> filter = new JComboBox<>(new String[]{
            "All", "Needs verification", "Pending", "Confirmed", "Completed", "Cancelled"});
    private final JTextField search = new JTextField();
    private final List<Booking> shown = new ArrayList<>();
    private final JLabel details = new JLabel(" ");
    private final JButton verifyBtn = Theme.primary("Verify payment");
    private final JButton rejectBtn = Theme.danger("Reject payment");
    private final JButton completeBtn = Theme.secondary("Mark completed");
    private final JButton cancelBtn = Theme.danger("Cancel booking");

    public ManageBookingsPanel(Dashboard dash) {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(6).setCellRenderer(new Theme.StatusRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new Theme.StatusRenderer());
        int[] widths = {70, 140, 70, 110, 160, 90, 90, 175, 130};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateDetails(); });

        filter.setFont(Theme.BODY);
        filter.addActionListener(e -> refresh());
        Theme.styleField(search);
        search.setPreferredSize(new Dimension(220, 34));
        search.addActionListener(e -> refresh());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.muted("Open a booking to check its payment, then verify it or update its status."),
                BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        tools.add(Theme.muted("Search"));
        tools.add(search);
        tools.add(filter);
        top.add(tools, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        add(Theme.tableScroll(table), BorderLayout.CENTER);

        Theme.Card bottom = new Theme.Card(new BorderLayout(0, 12));
        details.setFont(Theme.BODY);
        details.setForeground(Theme.TEXT);
        bottom.add(details, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(cancelBtn);
        buttons.add(completeBtn);
        buttons.add(rejectBtn);
        buttons.add(verifyBtn);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        verifyBtn.addActionListener(e -> {
            Booking b = selected();
            if (b == null) return;
            int ok = JOptionPane.showConfirmDialog(this,
                    "Verify the payment of " + Theme.money(b.getFee()) + "\nGCash reference: " + b.getRefNumber()
                            + "\n\nMake sure this reference appears in your GCash history.",
                    "Verify payment", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) { ds.verifyPayment(b); refresh(); }
        });
        rejectBtn.addActionListener(e -> {
            Booking b = selected();
            if (b == null) return;
            int ok = JOptionPane.showConfirmDialog(this,
                    "Reject this payment? The customer will be asked to pay again.",
                    "Reject payment", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) { ds.rejectPayment(b); refresh(); }
        });
        completeBtn.addActionListener(e -> {
            Booking b = selected();
            if (b != null) { ds.completeBooking(b); refresh(); }
        });
        cancelBtn.addActionListener(e -> {
            Booking b = selected();
            if (b == null) return;
            int ok = JOptionPane.showConfirmDialog(this,
                    "Cancel booking " + b.getId() + "? Its time slot will open up again.",
                    "Cancel booking", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) { ds.cancelBooking(b); refresh(); }
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
        String q = search.getText().trim().toLowerCase();
        shown.clear();
        model.setRowCount(0);
        for (Booking b : ds.allBookings()) {
            User u = ds.findUser(b.getCustomerEmail());
            String name = u == null ? b.getCustomerEmail() : u.getName();
            if ("Needs verification".equals(want)) {
                if (!(b.isActive() && b.getPaymentStatus() == Booking.PaymentStatus.PENDING_VERIFICATION)) continue;
            } else if (!"All".equals(want) && !b.getStatus().toString().equals(want)) {
                continue;
            }
            if (!q.isEmpty() && !(b.getId().toLowerCase().contains(q) || name.toLowerCase().contains(q)
                    || b.getCustomerEmail().toLowerCase().contains(q))) continue;
            shown.add(b);
            model.addRow(new Object[]{b.getId(), name, b.getCourt(), b.getDate().format(Theme.DAY_FMT),
                    b.getTimeRange(), Theme.money(b.getFee()), b.getStatus().toString(),
                    b.getPaymentStatus().toString(), b.getRefNumber().isEmpty() ? "-" : b.getRefNumber()});
        }
        int idx = keep == null ? -1 : shown.indexOf(keep);
        if (idx >= 0) table.setRowSelectionInterval(idx, idx);
        updateDetails();
    }

    private void updateDetails() {
        Booking b = selected();
        boolean needsCheck = b != null && b.isActive()
                && b.getPaymentStatus() == Booking.PaymentStatus.PENDING_VERIFICATION;
        verifyBtn.setEnabled(needsCheck);
        rejectBtn.setEnabled(needsCheck);
        completeBtn.setEnabled(b != null && b.getStatus() == Booking.Status.CONFIRMED);
        cancelBtn.setEnabled(b != null && (b.getStatus() == Booking.Status.PENDING
                || b.getStatus() == Booking.Status.CONFIRMED));
        if (b == null) {
            details.setText("No booking selected.");
            return;
        }
        User u = ds.findUser(b.getCustomerEmail());
        String who = u == null ? b.getCustomerEmail() : u.getName() + " (" + u.getPhone() + ")";
        details.setText("<html><b>" + b.getId() + "</b>  |  " + who + "<br>"
                + b.getCourt() + "  |  " + b.getDate().format(Theme.DATE_FMT) + "  |  " + b.getTimeRange()
                + "  |  " + Theme.money(b.getFee()) + "<br><span style='color:#7A716A'>Payment: "
                + b.getPaymentStatus() + (b.getRefNumber().isEmpty() ? "" : "  |  Ref " + b.getRefNumber())
                + "</span></html>");
    }
}
