import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Register Customer: saves the customer's information so they can book courts. */
public class RegisterDialog extends JDialog {
    private final DataStore ds = DataStore.get();
    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JPasswordField passField = new JPasswordField();
    private final JPasswordField confirmField = new JPasswordField();
    private final JLabel errorLabel = new JLabel(" ");
    private String registeredEmail;

    public RegisterDialog(Window owner) {
        super(owner, "Create your account", ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel head = new JPanel(new GridLayout(0, 1, 0, 2));
        head.setOpaque(false);
        head.add(Theme.label("Create account", Theme.TITLE, Theme.ACCENT));
        head.add(Theme.muted("Register once, then book a court any time."));
        root.add(head, BorderLayout.NORTH);

        for (JTextField f : new JTextField[]{nameField, emailField, phoneField, passField, confirmField}) {
            Theme.styleField(f);
        }
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 10));
        form.setOpaque(false);
        form.add(Theme.labeled("Full name", nameField));
        form.add(Theme.labeled("Email", emailField));
        form.add(Theme.labeled("Mobile number", phoneField));
        form.add(Theme.labeled("Password (at least 6 characters)", passField));
        form.add(Theme.labeled("Confirm password", confirmField));
        root.add(form, BorderLayout.CENTER);

        errorLabel.setFont(Theme.SMALL);
        errorLabel.setForeground(Theme.ERROR);
        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setOpaque(false);
        south.add(errorLabel, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        JButton cancel = Theme.secondary("Cancel");
        JButton create = Theme.primary("Create account");
        cancel.addActionListener(e -> dispose());
        create.addActionListener(e -> onCreate());
        buttons.add(cancel);
        buttons.add(create);
        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(create);
        setPreferredSize(new Dimension(400, 620));
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** The email that was just registered, or null if the person cancelled. */
    public String getRegisteredEmail() { return registeredEmail; }

    private void onCreate() {
        String error = ds.register(nameField.getText(), emailField.getText(), phoneField.getText(),
                new String(passField.getPassword()), new String(confirmField.getPassword()));
        if (error != null) {
            errorLabel.setText(error);
            return;
        }
        registeredEmail = emailField.getText().trim().toLowerCase();
        JOptionPane.showMessageDialog(this, "Account created. You can sign in now.",
                "Welcome", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
