package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private String paymentMethod;  // "Ví điện tử" (cho MoMo)

    @Column(name = "payment_status")
    private String paymentStatus = "Chờ thanh toán";  // "Chờ thanh toán", "Đã thanh toán", "Đã hủy", "Đã hết hạn"

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
    private String gateway = "momo";  // Default to "momo" since we only use MoMo

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;  // MoMo QR code URL

    @Column(name = "qr_expiry_time")
    private LocalDateTime qrExpiryTime;

    // NEW: Thêm payment_url riêng biệt với qrCode
    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;  // MoMo deep link URL

    @Column(name = "redirect_url")
    private String redirectUrl;  // URL để điều hướng sau khi thanh toán

    // FIXED: Remove insertable/updatable = false để có thể set callback URL
    @Column(name = "callback_url")
    private String callbackUrl;  // URL để nhận webhook từ MoMo

    // NEW: Thêm một số fields hữu ích cho MoMo
    @Column(name = "partner_code")
    private String partnerCode;  // MoMo partner code

    @Column(name = "order_id")
    private String orderId;  // MoMo order ID

    @Column(name = "request_id")
    private String requestId;  // MoMo request ID

    @Column(name = "signature")
    private String signature;  // MoMo signature for verification

    @Column(name = "extra_data")
    private String extraData;  // Additional data for MoMo

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods cho MoMo
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
}