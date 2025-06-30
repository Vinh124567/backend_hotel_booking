package com.example.demo.service.payment;

import com.example.demo.dto.payment.PaymentCallbackRequest;
import com.example.demo.dto.payment.PaymentRequest;
import com.example.demo.dto.payment.PaymentResponse;
import com.example.demo.dto.payment.PaymentStatusResponse;
import com.example.demo.entity.Payment;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    // Core payment operations
    PaymentResponse createPayment(PaymentRequest request);
    PaymentResponse getPaymentById(Long id);
    List<PaymentResponse> getPaymentsByBookingId(Long bookingId);

    // Status operations
    PaymentStatusResponse checkPaymentStatus(Long paymentId);
    PaymentResponse updatePaymentStatus(Long id, String status);

    // MoMo integration
    PaymentResponse processPaymentCallback(PaymentCallbackRequest request);
    String generateQRCode(Long paymentId);
    boolean verifyPaymentCallback(PaymentCallbackRequest request);

    // Utility
    void deletePayment(Long id);
    public PaymentResponse simulatePaymentSuccess(Long paymentId);

    Payment createDepositPayment(Long bookingId, BigDecimal amount, BigDecimal depositPercentage);
    Payment createRemainingPayment(Long bookingId, BigDecimal amount);
    Payment createFullPayment(Long bookingId, BigDecimal amount);
    Payment createRefundPayment(Long bookingId, BigDecimal amount, String reason);
}