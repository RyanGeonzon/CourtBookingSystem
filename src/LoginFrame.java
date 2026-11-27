import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.io.IOException;

public class LoginFrame extends JFrame {

    private final DataStore ds = DataStore.get();

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
        setSize(900, 600);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);          // center on screen
        getRootPane().setDefaultButton(signInButton);  // Enter key submits
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(320, 440));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Sign in");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(Theme.ACCENT);
        c.gridy = 0; c.insets = new Insets(0, 0, 4, 0);
        form.add(title, c);

        JLabel subtitle = new JLabel("Use the email you registered with.");
        subtitle.setForeground(Color.GRAY);
        c.gridy = 1; c.insets = new Insets(0, 0, 24, 0);
        form.add(subtitle, c);

        c.gridy = 2; c.insets = new Insets(0, 0, 6, 0);
        form.add(boldLabel("Email"), c);
        Theme.styleField(emailField);
        c.gridy = 3; c.insets = new Insets(0, 0, 16, 0);
        form.add(emailField, c);

        c.gridy = 4; c.insets = new Insets(0, 0, 6, 0);
        form.add(boldLabel("Password"), c);
        Theme.styleField(passwordField);
        c.gridy = 5; c.insets = new Insets(0, 0, 16, 0);
        form.add(passwordField, c);

        messageLabel.setForeground(Theme.ERROR);
        c.gridy = 6; c.insets = new Insets(0, 0, 12, 0);
        form.add(messageLabel, c);

        signInButton.setBackground(Theme.ACCENT);
        signInButton.setForeground(Color.WHITE);
        signInButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        signInButton.setFocusPainted(false);
        signInButton.setPreferredSize(new Dimension(0, 44));
        signInButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signInButton.addActionListener(e -> onSignIn());
        c.gridy = 7; c.insets = new Insets(0, 0, 0, 0);
        form.add(signInButton, c);

        // "Register Customer" entry point
        JButton register = new JButton("<html>New here? <u>Create an account</u></html>");
        register.setForeground(Theme.ACCENT_DARK);
        register.setFont(new Font("SansSerif", Font.PLAIN, 13));
        register.setContentAreaFilled(false);
        register.setBorderPainted(false);
        register.setFocusPainted(false);
        register.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        register.addActionListener(e -> onRegister());
        c.gridy = 8; c.insets = new Insets(14, 0, 0, 0);
        form.add(register, c);

        JLabel demo = new JLabel("<html><div style='text-align:center'>Demo logins<br>"
                + "Customer: player@court.com / hoops123<br>"
                + "Owner: owner@court.com / owner123</div></html>");
        demo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        demo.setForeground(Color.GRAY);
        demo.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 9; c.insets = new Insets(18, 0, 0, 0);
        form.add(demo, c);

        panel.add(form);
        return panel;
    }

    private JLabel boldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        return label;
    }

    private void onRegister() {
        RegisterDialog dialog = new RegisterDialog(this);
        dialog.setVisible(true);
        if (dialog.getRegisteredEmail() != null) {
            emailField.setText(dialog.getRegisteredEmail());
            passwordField.setText("");
            messageLabel.setForeground(Color.DARK_GRAY);
            messageLabel.setText("Account created. Enter your password.");
            passwordField.requestFocusInWindow();
        }
    }

    private void onSignIn() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty()) {
            showError("Enter your email address.", emailField);
            return;
        }
        if (!DataStore.isValidEmail(email)) {
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

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                Thread.sleep(500);                    // pretend to wait for a server
                return ds.authenticate(email, password);
            }

            @Override
            protected void done() {
                signInButton.setEnabled(true);
                try {
                    User user = get();
                    if (user != null) {
                        ds.setCurrentUser(user);
                        new Dashboard().setVisible(true);
                        dispose();
                    } else {
                        showError("Wrong email or password.", passwordField);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Something went wrong. Try again.", null);
                }
            }
        }.execute();
    }

    private void showError(String text, JComponent focusTarget) {
        messageLabel.setForeground(Theme.ERROR);
        messageLabel.setText(text);
        if (focusTarget != null) {
            focusTarget.requestFocusInWindow();
        }
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
