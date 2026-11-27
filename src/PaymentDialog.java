import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Random;

/** Make Payment: show the owner's GCash QR, then record the reference number the customer got from GCash. */
public class PaymentDialog extends JDialog {
    private final DataStore ds = DataStore.get();
    private final Booking booking;
    private final JTextField refField = new JTextField();
    private final JLabel errorLabel = new JLabel(" ");
    private boolean submitted;

    public PaymentDialog(Window owner, Booking booking) {
        super(owner, "Make payment - " + booking.getId(), ModalityType.APPLICATION_MODAL);
        this.booking = booking;

        JPanel root = new JPanel(new BorderLayout(24, 0));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(24, 24, 24, 24));
        root.add(buildQrColumn(), BorderLayout.WEST);
        root.add(buildDetailsColumn(), BorderLayout.CENTER);
        setContentPane(root);

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(null);
    }

    public boolean isSubmitted() { return submitted; }

    private JPanel buildQrColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 10));
        col.setOpaque(false);
        col.add(new QrPanel(), BorderLayout.CENTER);
        JLabel cap = Theme.muted("Scan with GCash");
        cap.setHorizontalAlignment(SwingConstants.CENTER);
        col.add(cap, BorderLayout.SOUTH);
        return col;
    }

    private JPanel buildDetailsColumn() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(360, 340));

        col.add(left(Theme.label("Pay with GCash", Theme.TITLE.deriveFont(22f), Theme.TEXT)));
        col.add(Box.createVerticalStrut(4));
        col.add(left(Theme.muted(booking.getId() + "  |  " + booking.getCourt() + "  |  "
                + booking.getDate().format(Theme.SHORT_DATE) + "  |  " + booking.getTimeRange())));
        col.add(Box.createVerticalStrut(14));

        JLabel amount = Theme.label(Theme.money(booking.getFee()), new Font("SansSerif", Font.BOLD, 30), Theme.ACCENT_DARK);
        col.add(left(Theme.muted("Amount to pay")));
        col.add(left(amount));
        col.add(Box.createVerticalStrut(12));

        JLabel steps = new JLabel("<html><div style='width:290px'>"
                + "1. Open GCash and scan the QR code.<br>"
                + "2. Send exactly the amount above.<br>"
                + "3. Copy the reference number from your GCash receipt and enter it below."
                + "</div></html>");
        steps.setFont(Theme.BODY);
        steps.setForeground(Theme.TEXT);
        col.add(left(steps));
        col.add(Box.createVerticalStrut(14));

        Theme.styleField(refField);
        JPanel refRow = Theme.labeled("GCash reference number", refField);
        refRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        col.add(left(refRow));

        errorLabel.setFont(Theme.SMALL);
        errorLabel.setForeground(Theme.ERROR);
        col.add(Box.createVerticalStrut(4));
        col.add(left(errorLabel));
        col.add(Box.createVerticalGlue());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        JButton cancel = Theme.secondary("Cancel");
        JButton submit = Theme.primary("Submit payment");
        cancel.addActionListener(e -> dispose());
        submit.addActionListener(e -> onSubmit());
        buttons.add(cancel);
        buttons.add(submit);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        col.add(left(buttons));

        getRootPane().setDefaultButton(submit);
        return col;
    }

    private JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private void onSubmit() {
        String ref = refField.getText().replaceAll("\\s", "");
        if (!ref.matches("\\d{10,20}")) {
            errorLabel.setText("Enter the reference number (10 to 20 digits).");
            refField.requestFocusInWindow();
            return;
        }
        if (ds.isRefUsed(ref, booking)) {
            errorLabel.setText("That reference number was already used.");
            refField.requestFocusInWindow();
            return;
        }
        ds.submitPayment(booking, ref);
        submitted = true;
        JOptionPane.showMessageDialog(this,
                "Payment recorded. The Owner will verify it,\nthen your booking will be confirmed.",
                "Payment submitted", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    /**
     * Shows gcash_qr.png if you place it next to the class files (same folder as the other images).
     * Until then it draws a placeholder pattern so the screen still looks right.
     */
    private static class QrPanel extends JPanel {
        private final Image qr = Theme.loadImage("gcash_qr.png");

        QrPanel() {
            setPreferredSize(new Dimension(240, 240));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int pad = 14;
            int size = Math.min(getWidth(), getHeight()) - pad * 2;
            if (qr != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(qr, pad, pad, size, size, null);
            } else {
                int n = 25;
                int cell = size / n;
                Random rnd = new Random(42);
                g2.setColor(Color.BLACK);
                for (int r = 0; r < n; r++) {
                    for (int c = 0; c < n; c++) {
                        if (inFinder(r, c, n)) continue;
                        if (rnd.nextInt(100) < 48) g2.fillRect(pad + c * cell, pad + r * cell, cell, cell);
                    }
                }
                finder(g2, pad, pad, cell);
                finder(g2, pad + (n - 7) * cell, pad, cell);
                finder(g2, pad, pad + (n - 7) * cell, cell);
            }
            g2.dispose();
        }

        private boolean inFinder(int r, int c, int n) {
            return (r < 8 && c < 8) || (r < 8 && c >= n - 8) || (r >= n - 8 && c < 8);
        }

        private void finder(Graphics2D g2, int x, int y, int cell) {
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, cell * 7, cell * 7);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + cell, y + cell, cell * 5, cell * 5);
            g2.setColor(Color.BLACK);
            g2.fillRect(x + cell * 2, y + cell * 2, cell * 3, cell * 3);
        }
    }
}
