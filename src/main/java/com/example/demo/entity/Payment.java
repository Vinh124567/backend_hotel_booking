package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // "Ví điện tử" (cho MoMo)

    @Column(name = "payment_status")
    private String paymentStatus = "Chờ thanh toán"; // "Chờ thanh toán", "Đã thanh toán", "Đã hủy", "Đã hết hạn"

    // ✅ NEW FIELDS FOR DEPOSIT PAYMENT
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType = PaymentType.THANH_TOAN_DAY_DU;

    @Column(name = "deposit_percentage", precision = 5, scale = 2)
    private BigDecimal depositPercentage;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private Set<PaymentHistory> histories = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "gateway")
    private String gateway = "momo"; // Default to "momo" since we only use MoMo

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode; // MoMo QR code URL

    @Column(name = "qr_expiry_time")
    private LocalDateTime qrExpiryTime;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl; // MoMo deep link URL

    @Column(name = "redirect_url")
    private String redirectUrl; // URL để điều hướng sau khi thanh toán

    @Column(name = "callback_url")
    private String callbackUrl; // URL để nhận webhook từ MoMo

    @Column(name = "partner_code")
    private String partnerCode; // MoMo partner code

    @Column(name = "order_id")
    private String orderId; // MoMo order ID

    @Column(name = "request_id")
    private String requestId; // MoMo request ID

    @Column(name = "signature")
    private String signature; // MoMo signature for verification

    @Column(name = "extra_data")
    private String extraData; // Additional data for MoMo

    // ✅ NEW ENUM FOR PAYMENT TYPES
    public enum PaymentType {
        COC_TRUOC("Cọc trước"),
        THANH_TOAN_DAY_DU("Thanh toán đầy đủ"),
        THANH_TOAN_CON_LAI("Thanh toán còn lại"),
        HOAN_TIEN("Hoàn tiền");

        private final String displayName;

        PaymentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Set default values for MoMo
        if (gateway == null) {
            gateway = "momo";
        }
        if (paymentMethod == null) {
            paymentMethod = "Ví điện tử";
        }
        if (paymentType == null) {
            paymentType = PaymentType.THANH_TOAN_DAY_DU;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ EXISTING HELPER METHODS (keep as is)
    public boolean isMoMoPayment() {
        return "momo".equalsIgnoreCase(gateway);
    }

    public boolean isExpired() {
        return qrExpiryTime != null && LocalDateTime.now().isAfter(qrExpiryTime);
    }

    public boolean isPaid() {
        return "Đã thanh toán".equals(paymentStatus);
    }

    public boolean isPending() {
        return "Chờ thanh toán".equals(paymentStatus);
    }

    public boolean isCancelled() {
        return "Đã hủy".equals(paymentStatus);
    }

    // ✅ NEW HELPER METHODS FOR DEPOSIT PAYMENT
    public boolean isDeposit() {
        return PaymentType.COC_TRUOC.equals(paymentType);
    }

    public boolean isFullPayment() {
        return PaymentType.THANH_TOAN_DAY_DU.equals(paymentType);
    }

    public boolean isRemainingPayment() {
        return PaymentType.THANH_TOAN_CON_LAI.equals(paymentType);
    }

    public boolean isRefund() {
        return PaymentType.HOAN_TIEN.equals(paymentType);
    }

    public String getPaymentTypeDisplay() {
        return paymentType != null ? paymentType.getDisplayName() : "Không xác định";
    }

    public String getPaymentStatusDisplay() {
        switch (paymentStatus) {
            case "Chờ thanh toán": return "🔄 Chờ thanh toán";
            case "Đã thanh toán": return "✅ Đã thanh toán";
            case "Đã hủy": return "❌ Đã hủy";
            case "Đã hết hạn": return "⏰ Đã hết hạn";
            default: return paymentStatus;
        }
    }

    public String getDepositInfo() {
        if (isDeposit() && depositPercentage != null) {
            return String.format("Cọc %.0f%% (%.0f VNĐ)",
                    depositPercentage.doubleValue(), amount.doubleValue());
        }
        return getPaymentTypeDisplay();
    }

    // ✅ Helper method to check if this payment is valid for a booking
    public boolean isValidForBooking(Long bookingId) {
        return booking != null && booking.getId().equals(bookingId) && isPaid();
    }

    // ✅ Helper method to get amount display
    public String getAmountDisplay() {
        if (isRefund()) {
            return String.format("-%,.0f VNĐ", amount.abs().doubleValue());
        } else {
            return String.format("%,.0f VNĐ", amount.doubleValue());
        }
    }

    // ✅ Helper method to check if payment can be refunded
    public boolean canBeRefunded() {
        return isPaid() && !isRefund() && paymentDate != null;
    }

    // ✅ Helper method to get payment summary for display
    public String getPaymentSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(getPaymentTypeDisplay());

        if (isDeposit() && depositPercentage != null) {
            summary.append(String.format(" (%.0f%%)", depositPercentage.doubleValue()));
        }

        summary.append(" - ").append(getAmountDisplay());
        summary.append(" - ").append(getPaymentStatusDisplay());

        return summary.toString();
    }

    // ✅ Static factory methods for easier creation
    public static Payment createDepositPayment(Booking booking, BigDecimal amount, BigDecimal depositPercentage) {
        return Payment.builder()
                .booking(booking)
                .amount(amount)
                .paymentType(PaymentType.COC_TRUOC)
                .depositPercentage(depositPercentage)
                .paymentMethod("Ví điện tử")
                .paymentStatus("Chờ thanh toán")
                .gateway("momo")
                .build();
    }

    public static Payment createFullPayment(Booking booking, BigDecimal amount) {
        return Payment.builder()
                .booking(booking)
                .amount(amount)
                .paymentType(PaymentType.THANH_TOAN_DAY_DU)
                .paymentMethod("Ví điện tử")
                .paymentStatus("Chờ thanh toán")
                .gateway("momo")
                .build();
    }

    public static Payment createRemainingPayment(Booking booking, BigDecimal amount) {
        return Payment.builder()
                .booking(booking)
                .amount(amount)
                .paymentType(PaymentType.THANH_TOAN_CON_LAI)
                .paymentMethod("Ví điện tử")
                .paymentStatus("Chờ thanh toán")
                .gateway("momo")
                .build();
    }

    public static Payment createRefund(Booking booking, BigDecimal amount, String notes) {
        return Payment.builder()
                .booking(booking)
                .amount(amount.negate()) // Negative for refund
                .paymentType(PaymentType.HOAN_TIEN)
                .paymentMethod("Ví điện tử")
                .paymentStatus("Đã hoàn tiền")
                .paymentDate(LocalDateTime.now())
                .notes(notes)
                .gateway("momo")
                .build();
    }
}