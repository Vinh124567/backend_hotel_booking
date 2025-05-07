package com.example.demo.service.payment;

import com.example.demo.dto.payment.PaymentCallbackRequest;
import com.example.demo.dto.payment.PaymentRequest;
import com.example.demo.dto.payment.PaymentResponse;
import com.example.demo.dto.payment.RefundRequest;

import java.util.List;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest request);

    PaymentResponse getPaymentById(Long id);

    List<PaymentResponse> getPaymentsByBookingId(Long bookingId);

    PaymentResponse updatePaymentStatus(Long id, String status);

    PaymentResponse processPaymentCallback(PaymentCallbackRequest request);

    PaymentResponse refundPayment(Long paymentId, RefundRequest request);

    void deletePayment(Long id);

    String generateQRCode(Long paymentId);

    boolean verifyPaymentCallback(PaymentCallbackRequest request);

    boolean checkPaymentStatus(Long paymentId);
}
