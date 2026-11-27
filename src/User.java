import java.time.LocalDate;

public class User {
    public enum Role {
        CUSTOMER("Customer"), OWNER("Owner");
        private final String label;
        Role(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final String email;
    private String password;      // plain text only because this is a demo without a database yet
    private String name;
    private String phone;
    private final Role role;
    private final LocalDate joined;

    public User(String email, String password, String name, String phone, Role role, LocalDate joined) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.joined = joined;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public LocalDate getJoined() { return joined; }
    public boolean isOwner() { return role == Role.OWNER; }
}
