package com.example.demo.dto.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentDate;    private String gateway;

    // MoMo specific fields
    private String qrCode;           // QR code URL
    private String paymentUrl;       // Deep link URL cho mobile app
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime qrExpiryTime;
    // Optional fields
    private String redirectUrl;
    private String orderId;          // MoMo order ID
    private String notes;

    // Helper methods
    public boolean isPaid() {
        return "Đã thanh toán".equals(paymentStatus);
    }

    public boolean isPending() {
        return "Chờ thanh toán".equals(paymentStatus);
    }

    public boolean isExpired() {
        return qrExpiryTime != null && LocalDateTime.now().isAfter(qrExpiryTime);
    }
}