import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Owner screen: View Customer Information, with each customer's booking history underneath. */
public class CustomersPanel extends JPanel implements Refreshable {
    private final DataStore ds = DataStore.get();
    private final DefaultTableModel custModel = new DefaultTableModel(
            new String[]{"Name", "Email", "Mobile", "Joined", "Bookings", "Total paid"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel histModel = new DefaultTableModel(
            new String[]{"ID", "Court", "Date", "Time", "Fee", "Booking", "Payment"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable custTable = new JTable(custModel);
    private final JTable histTable = new JTable(histModel);
    private final JTextField search = new JTextField();
    private final JLabel histTitle = Theme.label("Booking history", Theme.HEADING, Theme.TEXT);
    private final List<User> shown = new ArrayList<>();

    public CustomersPanel(Dashboard dash) {
        setBackground(Theme.BG);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(24, 28, 24, 28));

        Theme.styleTable(custTable);
        Theme.styleTable(histTable);
        custTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        histTable.getColumnModel().getColumn(5).setCellRenderer(new Theme.StatusRenderer());
        histTable.getColumnModel().getColumn(6).setCellRenderer(new Theme.StatusRenderer());
        custTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showHistory(); });

        Theme.styleField(search);
        search.setPreferredSize(new Dimension(240, 34));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.muted("Select a customer to see their bookings."), BorderLayout.WEST);
        JPanel s = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        s.setOpaque(false);
        s.add(Theme.muted("Search"));
        s.add(search);
        top.add(s, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel lower = new JPanel(new BorderLayout(0, 8));
        lower.setOpaque(false);
        lower.add(histTitle, BorderLayout.NORTH);
        lower.add(Theme.tableScroll(histTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, Theme.tableScroll(custTable), lower);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(12);
        split.setResizeWeight(0.55);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        refresh();
    }

    @Override
    public void refresh() {
        User keep = selected();
        String q = search.getText().trim().toLowerCase();
        shown.clear();
        custModel.setRowCount(0);
        for (User u : ds.customers()) {
            if (!q.isEmpty() && !(u.getName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q)
                    || u.getPhone().contains(q))) continue;
            List<Booking> list = ds.bookingsOf(u.getEmail());
            int count = 0;
            double paid = 0;
            for (Booking b : list) {
                if (b.isActive()) count++;
                if (b.isPaid()) paid += b.getFee();
            }
            shown.add(u);
            custModel.addRow(new Object[]{u.getName(), u.getEmail(), u.getPhone(),
                    u.getJoined().format(Theme.SHORT_DATE), count, Theme.money(paid)});
        }
        int idx = keep == null ? -1 : shown.indexOf(keep);
        if (idx >= 0) custTable.setRowSelectionInterval(idx, idx);
        showHistory();
    }

    private User selected() {
        int r = custTable.getSelectedRow();
        return (r >= 0 && r < shown.size()) ? shown.get(r) : null;
    }

    private void showHistory() {
        histModel.setRowCount(0);
        User u = selected();
        if (u == null) {
            histTitle.setText("Booking history");
            return;
        }
        histTitle.setText("Booking history - " + u.getName());
        for (Booking b : ds.bookingsOf(u.getEmail())) {
            histModel.addRow(new Object[]{b.getId(), b.getCourt(), b.getDate().format(Theme.DAY_FMT),
                    b.getTimeRange(), Theme.money(b.getFee()), b.getStatus().toString(),
                    b.getPaymentStatus().toString()});
        }
    }
}
