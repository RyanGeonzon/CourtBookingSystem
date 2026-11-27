import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Profile screen: see your details, edit your name and mobile number, and change your password. */
public class ProfilePanel extends JPanel implements Refreshable {
    private final Dashboard dash;
    private final DataStore ds = DataStore.get();

    private final Theme.Avatar avatar = new Theme.Avatar("?", 96);
    private final JLabel nameLabel = Theme.label(" ", new Font("SansSerif", Font.BOLD, 20), Theme.TEXT);
    private final JLabel emailLabel = Theme.muted(" ");
    private final JLabel roleLabel = Theme.label(" ", Theme.BOLD, Theme.ACCENT_DARK);
    private final JLabel sinceLabel = Theme.muted(" ");
    private final JPanel statsBox = new JPanel(new GridLayout(0, 1, 0, 10));

    private final JTextField nameField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JLabel infoMsg = new JLabel(" ");

    private final JPasswordField currentPw = new JPasswordField();
    private final JPasswordField newPw = new JPasswordField();
    private final JPasswordField confirmPw = new JPasswordField();
    private final JLabel pwMsg = new JLabel(" ");

    public ProfilePanel(Dashboard dash) {
        this.dash = dash;
        setBackground(Theme.BG);
        setLayout(new BorderLayout());

        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(24, 28, 24, 28));
        body.add(buildSummaryCard(), BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JComponent info = buildInfoCard();
        JComponent pw = buildPasswordCard();
        info.setAlignmentX(LEFT_ALIGNMENT);
        pw.setAlignmentX(LEFT_ALIGNMENT);
        right.add(info);
        right.add(Box.createVerticalStrut(18));
        right.add(pw);
        JPanel rightHolder = new JPanel(new BorderLayout());
        rightHolder.setOpaque(false);
        rightHolder.add(right, BorderLayout.NORTH);
        body.add(rightHolder, BorderLayout.CENTER);

        add(Theme.pageScroll(body), BorderLayout.CENTER);
        refresh();
    }

    private JComponent buildSummaryCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 16));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{avatar, nameLabel, emailLabel, roleLabel, sinceLabel}) {
            c.setAlignmentX(CENTER_ALIGNMENT);
        }
        top.add(avatar);
        top.add(Box.createVerticalStrut(12));
        top.add(nameLabel);
        top.add(Box.createVerticalStrut(2));
        top.add(emailLabel);
        top.add(Box.createVerticalStrut(8));
        top.add(roleLabel);
        top.add(Box.createVerticalStrut(2));
        top.add(sinceLabel);
        card.add(top, BorderLayout.NORTH);

        statsBox.setOpaque(false);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        JPanel pad = new JPanel(new BorderLayout());
        pad.setOpaque(false);
        pad.setBorder(new EmptyBorder(16, 0, 0, 0));
        pad.add(statsBox, BorderLayout.NORTH);
        holder.add(pad, BorderLayout.CENTER);
        card.add(holder, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(290, 0));
        wrap.add(card, BorderLayout.NORTH);
        return wrap;
    }

    private JComponent buildInfoCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.add(Theme.label("Personal information", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);

        Theme.styleField(nameField);
        Theme.styleField(phoneField);
        Theme.styleField(emailField);
        emailField.setEditable(false);
        emailField.setBackground(new Color(0xF3EFEA));
        emailField.setForeground(Theme.MUTED);

        JPanel form = new JPanel(new GridLayout(0, 2, 14, 12));
        form.setOpaque(false);
        form.add(Theme.labeled("Full name", nameField));
        form.add(Theme.labeled("Mobile number", phoneField));
        form.add(Theme.labeled("Email (cannot be changed)", emailField));
        card.add(form, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        infoMsg.setFont(Theme.SMALL);
        south.add(infoMsg, BorderLayout.CENTER);
        JButton save = Theme.primary("Save changes");
        save.addActionListener(e -> onSaveInfo());
        south.add(save, BorderLayout.EAST);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildPasswordCard() {
        Theme.Card card = new Theme.Card(new BorderLayout(0, 14));
        card.add(Theme.label("Change password", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);

        Theme.styleField(currentPw);
        Theme.styleField(newPw);
        Theme.styleField(confirmPw);
        JPanel form = new JPanel(new GridLayout(1, 3, 14, 0));
        form.setOpaque(false);
        form.add(Theme.labeled("Current password", currentPw));
        form.add(Theme.labeled("New password", newPw));
        form.add(Theme.labeled("Confirm new password", confirmPw));
        card.add(form, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        pwMsg.setFont(Theme.SMALL);
        south.add(pwMsg, BorderLayout.CENTER);
        JButton update = Theme.secondary("Update password");
        update.addActionListener(e -> onChangePassword());
        south.add(update, BorderLayout.EAST);
        card.add(south, BorderLayout.SOUTH);
        return card;
    }

    @Override
    public void refresh() {
        User u = ds.getCurrentUser();
        if (u == null) return;
        avatar.setPerson(u.getName());
        nameLabel.setText(u.getName());
        emailLabel.setText(u.getEmail());
        roleLabel.setText(u.getRole().toString().toUpperCase());
        sinceLabel.setText("Member since " + u.getJoined().format(Theme.SHORT_DATE));
        nameField.setText(u.getName());
        phoneField.setText(u.getPhone());
        emailField.setText(u.getEmail());
        infoMsg.setText(" ");
        pwMsg.setText(" ");
        currentPw.setText("");
        newPw.setText("");
        confirmPw.setText("");

        statsBox.removeAll();
        if (u.isOwner()) {
            int active = 0;
            for (Booking b : ds.allBookings()) if (b.isActive()) active++;
            statsBox.add(statRow("Customers", String.valueOf(ds.customers().size())));
            statsBox.add(statRow("Bookings handled", String.valueOf(active)));
            statsBox.add(statRow("Verified revenue", Theme.money(ds.revenue())));
        } else {
            List<Booking> mine = ds.bookingsOf(u.getEmail());
            int total = 0, upcoming = 0;
            double paid = 0;
            for (Booking b : mine) {
                if (b.isActive()) total++;
                if (b.isUpcoming()) upcoming++;
                if (b.isPaid()) paid += b.getFee();
            }
            statsBox.add(statRow("Total bookings", String.valueOf(total)));
            statsBox.add(statRow("Upcoming", String.valueOf(upcoming)));
            statsBox.add(statRow("Total paid", Theme.money(paid)));
        }
        statsBox.revalidate();
        statsBox.repaint();
    }

    private JPanel statRow(String key, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(Theme.label(key, Theme.BODY, Theme.MUTED), BorderLayout.WEST);
        p.add(Theme.label(value, Theme.BOLD, Theme.TEXT), BorderLayout.EAST);
        return p;
    }

    private void onSaveInfo() {
        String error = ds.updateProfile(ds.getCurrentUser(), nameField.getText(), phoneField.getText());
        if (error != null) {
            infoMsg.setForeground(Theme.ERROR);
            infoMsg.setText(error);
            return;
        }
        refresh();
        dash.refreshUser();
        infoMsg.setForeground(Theme.SUCCESS);
        infoMsg.setText("Profile updated.");
    }

    private void onChangePassword() {
        String error = ds.changePassword(ds.getCurrentUser(), new String(currentPw.getPassword()),
                new String(newPw.getPassword()), new String(confirmPw.getPassword()));
        if (error != null) {
            pwMsg.setForeground(Theme.ERROR);
            pwMsg.setText(error);
            return;
        }
        currentPw.setText("");
        newPw.setText("");
        confirmPw.setText("");
        pwMsg.setForeground(Theme.SUCCESS);
        pwMsg.setText("Password updated.");
    }
}
