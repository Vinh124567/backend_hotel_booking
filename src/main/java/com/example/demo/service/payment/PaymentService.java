package com.example.demo.service.payment;

import com.example.demo.dto.payment.PaymentCallbackRequest;
import com.example.demo.dto.payment.PaymentRequest;
import com.example.demo.dto.payment.PaymentResponse;
import com.example.demo.dto.payment.PaymentStatusResponse;

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
}