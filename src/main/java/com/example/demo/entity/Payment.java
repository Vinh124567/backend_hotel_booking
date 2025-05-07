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
    private String paymentMethod;  // "Thẻ tín dụng", "Thẻ ghi nợ", "Ví điện tử", "Mã QR"

    @Column(name = "payment_status")
    private String paymentStatus = "Chờ thanh toán";  // "Chờ thanh toán", "Đã thanh toán", "Đã hủy"

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
    private String gateway;  // "ZaloPay", "VNPay", etc.

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;  // Lưu URL hình ảnh QR hoặc dữ liệu QR

    @Column(name = "qr_expiry_time")
    private LocalDateTime qrExpiryTime;

    @Column(name = "redirect_url")
    private String redirectUrl;  // URL để điều hướng sau khi thanh toán

    @Column(name = "callback_url")
    private String callbackUrl;  // URL để nhận webhook từ cổng thanh toán

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}