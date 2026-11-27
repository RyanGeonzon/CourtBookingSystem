import java.time.LocalDate;
import java.time.LocalDateTime;

public class Booking {
    public enum Status {
        PENDING("Pending"), CONFIRMED("Confirmed"), COMPLETED("Completed"), CANCELLED("Cancelled");
        private final String label;
        Status(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum PaymentStatus {
        UNPAID("Unpaid"), PENDING_VERIFICATION("Awaiting verification"),
        VERIFIED("Verified"), REJECTED("Rejected");
        private final String label;
        PaymentStatus(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final String id;
    private final String customerEmail;
    private final String court;
    private final LocalDate date;
    private final int startHour;
    private final int hours;
    private final double fee;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private Status status = Status.PENDING;
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private String refNumber = "";

    public Booking(String id, String customerEmail, String court, LocalDate date,
                   int startHour, int hours, double fee) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.court = court;
        this.date = date;
        this.startHour = startHour;
        this.hours = hours;
        this.fee = fee;
    }

    public String getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCourt() { return court; }
    public LocalDate getDate() { return date; }
    public int getStartHour() { return startHour; }
    public int getHours() { return hours; }
    public double getFee() { return fee; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus p) { this.paymentStatus = p; }
    public String getRefNumber() { return refNumber; }
    public void setRefNumber(String refNumber) { this.refNumber = refNumber; }

    public String getTimeRange() { return Theme.range(startHour, hours); }

    /** A cancelled booking frees its slot. Everything else holds the slot. */
    public boolean isActive() { return status != Status.CANCELLED; }

    public boolean isUpcoming() {
        return isActive() && status != Status.COMPLETED && !date.isBefore(LocalDate.now());
    }

    /** True when the customer still has to pay (or pay again after a rejection). */
    public boolean canPay() {
        return status == Status.PENDING
                && (paymentStatus == PaymentStatus.UNPAID || paymentStatus == PaymentStatus.REJECTED);
    }

    public boolean isPaid() { return isActive() && paymentStatus == PaymentStatus.VERIFIED; }

    public boolean covers(String court, LocalDate date, int hour) {
        return this.court.equals(court) && this.date.equals(date)
                && hour >= startHour && hour < startHour + hours;
    }
}
