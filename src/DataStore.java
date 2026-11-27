import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Temporary in-memory "database" so the whole GUI works before Part 5 (Database Design).
 * When you add a real database, replace the method bodies here and the screens stay the same.
 */
public class DataStore {
    private static final DataStore INSTANCE = new DataStore();

    public static DataStore get() { return INSTANCE; }

    public enum SlotState { AVAILABLE, BOOKED, BLOCKED, PAST }

    private final List<User> users = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final Set<String> blocked = new HashSet<>();
    private final List<String> courts = Arrays.asList("Court 1", "Court 2");

    // Court schedule settings (the Owner can change these)
    private int openHour = 8;
    private int closeHour = 22;
    private int peakStart = 17;
    private double regularRate = 300;
    private double peakRate = 400;

    private int nextId = 1001;
    private User currentUser;

    private DataStore() {
        seed();
    }

    // =====================================================================
    // Users
    // =====================================================================
    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User u) { this.currentUser = u; }

    public synchronized User findUser(String email) {
        if (email == null) return null;
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email.trim())) return u;
        }
        return null;
    }

    public synchronized User authenticate(String email, String password) {
        User u = findUser(email);
        return (u != null && u.getPassword().equals(password)) ? u : null;
    }

    public static boolean isValidEmail(String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    /** Returns null on success, or the error message to show. */
    public synchronized String register(String name, String email, String phone,
                                        String password, String confirm) {
        name = name.trim();
        email = email.trim().toLowerCase();
        phone = phone.trim();
        if (name.length() < 2) return "Enter your full name.";
        if (!isValidEmail(email)) return "That email doesn't look right.";
        if (findUser(email) != null) return "That email is already registered.";
        if (!phone.matches("[0-9 +\\-]{7,15}")) return "Enter a valid phone number.";
        if (password.length() < 6) return "Password must be at least 6 characters.";
        if (!password.equals(confirm)) return "Passwords do not match.";
        users.add(new User(email, password, name, phone, User.Role.CUSTOMER, LocalDate.now()));
        return null;
    }

    public synchronized String updateProfile(User u, String name, String phone) {
        name = name.trim();
        phone = phone.trim();
        if (name.length() < 2) return "Enter your full name.";
        if (!phone.matches("[0-9 +\\-]{7,15}")) return "Enter a valid phone number.";
        u.setName(name);
        u.setPhone(phone);
        return null;
    }

    public synchronized String changePassword(User u, String current, String next, String confirm) {
        if (!u.getPassword().equals(current)) return "Current password is wrong.";
        if (next.length() < 6) return "New password must be at least 6 characters.";
        if (!next.equals(confirm)) return "New passwords do not match.";
        u.setPassword(next);
        return null;
    }

    public synchronized List<User> customers() {
        List<User> list = new ArrayList<>();
        for (User u : users) if (!u.isOwner()) list.add(u);
        list.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    // =====================================================================
    // Court schedule settings and fees
    // =====================================================================
    public List<String> getCourts() { return courts; }
    public int getOpenHour() { return openHour; }
    public int getCloseHour() { return closeHour; }
    public int getPeakStart() { return peakStart; }
    public double getRegularRate() { return regularRate; }
    public double getPeakRate() { return peakRate; }

    public synchronized String updateSettings(int open, int close, int peak, double regular, double peakR) {
        if (open >= close) return "Opening time must be before closing time.";
        if (regular <= 0 || peakR <= 0) return "Rates must be greater than zero.";
        openHour = open;
        closeHour = close;
        peakStart = peak;
        regularRate = regular;
        peakRate = peakR;
        return null;
    }

    public double rateFor(int hour) {
        return hour >= peakStart ? peakRate : regularRate;
    }

    /** Calculate Booking Fee: add up the hourly rate of every hour in the schedule. */
    public double computeFee(int start, int hours) {
        double total = 0;
        for (int h = start; h < start + hours; h++) total += rateFor(h);
        return total;
    }

    // =====================================================================
    // Availability
    // =====================================================================
    private static String key(String court, LocalDate date, int hour) {
        return court + "|" + date + "|" + hour;
    }

    public synchronized SlotState slotState(String court, LocalDate date, int hour) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || (date.equals(today) && hour <= LocalTime.now().getHour())) {
            return SlotState.PAST;
        }
        if (bookingAt(court, date, hour) != null) return SlotState.BOOKED;
        if (blocked.contains(key(court, date, hour))) return SlotState.BLOCKED;
        return SlotState.AVAILABLE;
    }

    public synchronized Booking bookingAt(String court, LocalDate date, int hour) {
        for (Booking b : bookings) {
            if (b.isActive() && b.covers(court, date, hour)) return b;
        }
        return null;
    }

    public synchronized boolean isRangeAvailable(String court, LocalDate date, int start, int hours) {
        if (hours <= 0 || start < openHour || start + hours > closeHour) return false;
        for (int h = start; h < start + hours; h++) {
            if (slotState(court, date, h) != SlotState.AVAILABLE) return false;
        }
        return true;
    }

    /** Owner blocks or reopens a slot (maintenance, private event). */
    public synchronized void toggleBlock(String court, LocalDate date, int hour) {
        String k = key(court, date, hour);
        if (!blocked.remove(k)) blocked.add(k);
    }

    // =====================================================================
    // Bookings and payments
    // =====================================================================
    private static final Comparator<Booking> NEWEST_FIRST =
            Comparator.comparing(Booking::getDate).thenComparing(Booking::getStartHour).reversed();

    public synchronized List<Booking> allBookings() {
        List<Booking> list = new ArrayList<>(bookings);
        list.sort(NEWEST_FIRST);
        return list;
    }

    public synchronized List<Booking> bookingsOf(String email) {
        List<Booking> list = new ArrayList<>();
        for (Booking b : bookings) if (b.getCustomerEmail().equalsIgnoreCase(email)) list.add(b);
        list.sort(NEWEST_FIRST);
        return list;
    }

    /** Book Basketball Court. Returns null if the slot was taken in the meantime. */
    public synchronized Booking createBooking(User customer, String court, LocalDate date, int start, int hours) {
        if (!isRangeAvailable(court, date, start, hours)) return null;
        Booking b = new Booking("BK-" + (nextId++), customer.getEmail(), court, date, start, hours,
                computeFee(start, hours));
        bookings.add(b);
        return b;
    }

    public synchronized boolean isRefUsed(String ref, Booking except) {
        for (Booking b : bookings) {
            if (b != except && b.getRefNumber().equals(ref)) return true;
        }
        return false;
    }

    /** Make Payment: the customer paid through GCash and enters the reference number. */
    public synchronized void submitPayment(Booking b, String ref) {
        b.setRefNumber(ref);
        b.setPaymentStatus(Booking.PaymentStatus.PENDING_VERIFICATION);
    }

    public synchronized void verifyPayment(Booking b) {
        b.setPaymentStatus(Booking.PaymentStatus.VERIFIED);
        if (b.getStatus() == Booking.Status.PENDING) b.setStatus(Booking.Status.CONFIRMED);
    }

    public synchronized void rejectPayment(Booking b) {
        b.setPaymentStatus(Booking.PaymentStatus.REJECTED);
    }

    public synchronized void completeBooking(Booking b) { b.setStatus(Booking.Status.COMPLETED); }

    public synchronized void cancelBooking(Booking b) { b.setStatus(Booking.Status.CANCELLED); }

    public synchronized double revenue() {
        double total = 0;
        for (Booking b : bookings) if (b.isPaid()) total += b.getFee();
        return total;
    }

    // =====================================================================
    // Demo data
    // =====================================================================
    private void seed() {
        users.add(new User("owner@court.com", "owner123", "Dennis Reyes", "0917 555 0101",
                User.Role.OWNER, LocalDate.now().minusYears(1)));
        users.add(new User("player@court.com", "hoops123", "Juan Dela Cruz", "0917 123 4567",
                User.Role.CUSTOMER, LocalDate.now().minusMonths(2)));
        users.add(new User("maria@court.com", "maria123", "Maria Santos", "0928 555 0198",
                User.Role.CUSTOMER, LocalDate.now().minusDays(20)));

        LocalDate today = LocalDate.now();
        Booking a = seedBooking("player@court.com", "Court 1", today.minusDays(1), 18, 2);
        a.setRefNumber("7001234567890");
        a.setPaymentStatus(Booking.PaymentStatus.VERIFIED);
        a.setStatus(Booking.Status.COMPLETED);

        Booking b = seedBooking("player@court.com", "Court 1", today.plusDays(1), 17, 2);
        b.setRefNumber("7001234500111");
        verifyPayment(b);

        seedBooking("player@court.com", "Court 2", today.plusDays(2), 19, 1);   // Pending, unpaid

        Booking c = seedBooking("maria@court.com", "Court 2", today.plusDays(1), 9, 2);
        submitPayment(c, "7009876543210");                                      // waiting for the Owner

        Booking d = seedBooking("maria@court.com", "Court 1", today.plusDays(3), 15, 1);
        d.setRefNumber("7005550001234");
        verifyPayment(d);
    }

    private Booking seedBooking(String email, String court, LocalDate date, int start, int hours) {
        Booking b = new Booking("BK-" + (nextId++), email, court, date, start, hours, computeFee(start, hours));
        bookings.add(b);
        return b;
    }
}
