import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.io.IOException;

public class LoginFrame extends JFrame {

    private static final Color NAVY = new Color(0xE27C3C);
    private static final Color ERROR = new Color(0xB3261E);

    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton signInButton = new JButton("Sign in");

    private Image loadImage(String path) {
        try {
            return ImageIO.read(getClass().getResource(path));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Could not load " + path + ": " + e.getMessage());
            return null;
        }
    }

    public LoginFrame() {
        super("Courtside Booking - Sign in");
        Image logo = loadImage("logo.jpg");
        if (logo != null) {
            setIconImage(logo);
        }
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(1, 2));      // two equal columns
        add(new CourtPanel());
        add(buildFormPanel());
        setSize(900, 560);
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);          // center on screen
        getRootPane().setDefaultButton(signInButton);  // Enter key submits
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(320, 340));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Sign in");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(NAVY);
        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);
        form.add(title, c);

        JLabel subtitle = new JLabel("Use the email you registered with.");
        subtitle.setForeground(Color.GRAY);
        c.gridy = 1; c.insets = new Insets(0, 0, 24, 0);
        form.add(subtitle, c);

        c.gridy = 2; c.insets = new Insets(0, 0, 6, 0);
        form.add(boldLabel("Email"), c);
        styleField(emailField);
        c.gridy = 3; c.insets = new Insets(0, 0, 16, 0);
        form.add(emailField, c);

        c.gridy = 4; c.insets = new Insets(0, 0, 6, 0);
        form.add(boldLabel("Password"), c);
        styleField(passwordField);
        c.gridy = 5; c.insets = new Insets(0, 0, 16, 0);
        form.add(passwordField, c);

        messageLabel.setForeground(ERROR);
        c.gridy = 6; c.insets = new Insets(0, 0, 12, 0);
        form.add(messageLabel, c);

        signInButton.setBackground(NAVY);
        signInButton.setForeground(Color.WHITE);
        signInButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        signInButton.setFocusPainted(false);
        signInButton.setPreferredSize(new Dimension(0, 44));
        signInButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signInButton.addActionListener(e -> onSignIn());
        c.gridy = 7; c.insets = new Insets(0, 0, 0, 0);
        form.add(signInButton, c);

        panel.add(form);
        return panel;
    }

    private JLabel boldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        return label;
    }

    private void styleField(JTextField field) {
        field.setPreferredSize(new Dimension(0, 40));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xFCFCFC), 1),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
    }

    private void onSignIn() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty()) {
            showError("Enter your email address.", emailField);
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            showError("That email doesn't look right.", emailField);
            return;
        }
        if (password.isEmpty()) {
            showError("Enter your password.", passwordField);
            return;
        }

        messageLabel.setForeground(Color.DARK_GRAY);
        messageLabel.setText("Signing in...");
        signInButton.setEnabled(false);

        // Run the slow check off the UI thread so the window doesn't freeze
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Thread.sleep(800);                    // pretend to wait for a server
                return authenticate(email, password);
            }

            @Override
            protected void done() {
                signInButton.setEnabled(true);
                try {
                    if (get()) {
                        messageLabel.setForeground(Color.DARK_GRAY);
                        messageLabel.setText("Signed in. Opening bookings...");
                        // TODO: open your booking window here
                    } else {
                        showError("Wrong email or password.", passwordField);
                    }
                } catch (Exception ex) {
                    showError("Something went wrong. Try again.", null);
                }
            }
        }.execute();
    }

    // Demo only. Replace with a database or API check.
    private boolean authenticate(String email, String password) {
        return email.equals("player@court.com") && password.equals("hoops123");
    }

    private void showError(String text, JComponent focusTarget) {
        messageLabel.setForeground(ERROR);
        messageLabel.setText(text);
        if (focusTarget != null) {
            focusTarget.requestFocusInWindow();
        }
    }

    public static void main(String[] args) {
        try {
            // Makes button colors look the same on Windows, Mac and Linux
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
