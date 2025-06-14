package com.example.demo.service.payment;

import com.example.demo.config.PaymentConfig;
import com.example.demo.dto.payment.*;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentHistory;
import com.example.demo.event.PaymentSuccessEvent;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentHistoryRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.momo.MoMoPaymentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final MoMoPaymentService moMoPaymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentConfig paymentConfig;

    @Transactional
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại với id: " + request.getBookingId()));

        Payment payment = createPaymentEntity(booking, request);
        payment = paymentRepository.save(payment);

        try {
            MoMoPaymentResponse momoResponse = moMoPaymentService.createPaymentRequest(payment);
            updatePaymentWithMoMoResponse(payment, momoResponse);
            payment = paymentRepository.save(payment);

        } catch (Exception e) {
            log.error("Lỗi tạo MoMo payment request cho payment: " + payment.getId(), e);
            throw new RuntimeException("Không thể tạo thanh toán MoMo: " + e.getMessage());
        }

        paymentHistoryRepository.save(PaymentHistory.createRecord(payment, "Khởi tạo thanh toán MoMo"));
        return modelMapper.map(payment, PaymentResponse.class);
    }

    @Override
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + id));
        return modelMapper.map(payment, PaymentResponse.class);
    }

    @Override
    public List<PaymentResponse> getPaymentsByBookingId(Long bookingId) {
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        return payments.stream()
                .map(payment -> modelMapper.map(payment, PaymentResponse.class))
                .toList();
    }

    @Override
    public PaymentStatusResponse checkPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + paymentId));

        if (payment.isPaid()) {
            return PaymentStatusResponse.success(paymentId);
        }

        if (payment.isExpired()) {
            updatePaymentStatus(paymentId, "Đã hết hạn");
            return PaymentStatusResponse.expired(paymentId);
        }

        try {
            boolean isPaidAtMoMo = moMoPaymentService.checkPaymentStatus(payment);
            if (isPaidAtMoMo) {
                updatePaymentStatus(paymentId, "Đã thanh toán");
                return PaymentStatusResponse.success(paymentId);
            }
        } catch (Exception e) {
            log.error("Error checking MoMo status for payment: " + paymentId, e);
        }

        return PaymentStatusResponse.pending(paymentId);
    }

    @Transactional
    @Override
    public PaymentResponse updatePaymentStatus(Long id, String status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + id));

        String oldStatus = payment.getPaymentStatus();
        payment.setPaymentStatus(status);

        if ("Đã thanh toán".equals(status)) {
            payment.setPaymentDate(LocalDateTime.now());
            publishPaymentSuccessEvent(payment);
        }

        payment = paymentRepository.save(payment);

        paymentHistoryRepository.save(
                PaymentHistory.updateRecord(payment,
                        "Manual status update from " + oldStatus + " to " + status)
        );

        return modelMapper.map(payment, PaymentResponse.class);
    }

    @Transactional
    @Override
    public PaymentResponse processPaymentCallback(PaymentCallbackRequest request) {
        if (!moMoPaymentService.verifyPaymentCallback(request)) {
            throw new RuntimeException("MoMo callback không hợp lệ");
        }

        Payment payment = findPaymentByCallback(request);
        String newPaymentStatus = request.isSuccess() ? "Đã thanh toán" : "Đã hủy";

        updatePaymentFromCallback(payment, request, newPaymentStatus);
        payment = paymentRepository.save(payment);

        paymentHistoryRepository.save(
                PaymentHistory.callbackRecord(payment,
                        "MoMo callback: " + request.getMessage(),
                        request.toString())
        );

        if (request.isSuccess()) {
            publishPaymentSuccessEvent(payment);
        }

        return modelMapper.map(payment, PaymentResponse.class);
    }

    @Override
    public String generateQRCode(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại"));
        return payment.getQrCode();
    }

    @Override
    public boolean verifyPaymentCallback(PaymentCallbackRequest request) {
        return moMoPaymentService.verifyPaymentCallback(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + id));
        paymentRepository.delete(payment);
    }

    @Transactional
    public PaymentResponse simulatePaymentSuccess(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + paymentId));

        String oldStatus = payment.getPaymentStatus();
        payment.setPaymentStatus("Đã thanh toán");
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponse("TEST: Simulated success");

        payment = paymentRepository.save(payment);

        paymentHistoryRepository.save(
                PaymentHistory.updateRecord(payment,
                        "TEST: Simulated success from " + oldStatus + " to Đã thanh toán")
        );

        publishPaymentSuccessEvent(payment);
        return modelMapper.map(payment, PaymentResponse.class);
    }

    private Payment createPaymentEntity(Booking booking, PaymentRequest request) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setGateway(request.getGateway());
        payment.setPaymentStatus("Chờ thanh toán");
        payment.setNotes(request.getNotes());
        payment.setTransactionId(moMoPaymentService.generateTransactionId());
        payment.setOrderId(moMoPaymentService.generateOrderId());
        payment.setRequestId(moMoPaymentService.generateRequestId());
        payment.setPartnerCode(paymentConfig.getMomoPartnerCode());
        payment.setQrExpiryTime(LocalDateTime.now().plusMinutes(15));
        payment.setCallbackUrl(request.getCallbackUrl() != null ?
                request.getCallbackUrl() : paymentConfig.getMomoCallbackUrl());
        payment.setRedirectUrl(request.getRedirectUrl() != null ?
                request.getRedirectUrl() : paymentConfig.getMomoRedirectUrl());
        return payment;
    }

    private void updatePaymentWithMoMoResponse(Payment payment, MoMoPaymentResponse momoResponse) {
        payment.setQrCode(momoResponse.getQrCodeUrl());
        payment.setPaymentUrl(momoResponse.getPayUrl());
        payment.setSignature(momoResponse.getSignature());
    }

    private void updatePaymentFromCallback(Payment payment, PaymentCallbackRequest request, String newStatus) {
        payment.setPaymentStatus(newStatus);
        payment.setGatewayResponse(request.getMessage());

        if (request.isSuccess()) {
            payment.setPaymentDate(LocalDateTime.now());
            if (request.getTransId() != null && !request.getTransId().isEmpty()) {
                payment.setTransactionId(request.getTransId());
            }
        }
    }

    private Payment findPaymentByCallback(PaymentCallbackRequest request) {
        return paymentRepository.findByOrderId(request.getOrderId())
                .or(() -> paymentRepository.findByTransactionId(request.getTransactionId()))
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại cho orderId: " + request.getOrderId()));
    }

    private void publishPaymentSuccessEvent(Payment payment) {
        try {
            eventPublisher.publishEvent(new PaymentSuccessEvent(payment.getBooking().getId()));
        } catch (Exception e) {
            log.error("Failed to publish payment success event for booking: " + payment.getBooking().getId(), e);
            paymentHistoryRepository.save(
                    PaymentHistory.createRecord(payment,
                            "ERROR: Payment successful but event publication failed: " + e.getMessage())
            );
        }
    }
}