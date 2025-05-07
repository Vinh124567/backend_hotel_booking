package com.example.demo.service.payment;

import com.example.demo.config.PaymentConfig;
import com.example.demo.dto.payment.PaymentCallbackRequest;
import com.example.demo.dto.payment.PaymentRequest;
import com.example.demo.dto.payment.PaymentResponse;
import com.example.demo.dto.payment.RefundRequest;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentHistoryRepository;
import com.example.demo.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final PaymentConfig paymentConfig;

    // Có thể thêm các service khác cho cổng thanh toán cụ thể
    // private final VNPayService vnPayService;
    // private final ZaloPayService zaloPayService;

    @PreAuthorize("hasRole('USER')")
    @Transactional
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // Kiểm tra booking có tồn tại không
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại với id: " + request.getBookingId()));

        // Tạo payment mới
        Payment payment = modelMapper.map(request, Payment.class);
        payment.setBooking(booking);
        payment.setPaymentStatus("Chờ thanh toán");


        // Tạo mã giao dịch
        payment.setTransactionId(generateTransactionId());

        // Thời gian hết hạn QR code (15 phút)
        payment.setQrExpiryTime(LocalDateTime.now().plusMinutes(15));

        // Lưu payment
        payment = paymentRepository.save(payment);

        // Tạo lịch sử thanh toán
        createPaymentHistory(payment, "Khởi tạo thanh toán", "create");

        // Tạo QR code dựa vào cổng thanh toán
        String qrCode = generateQRCode(payment.getId());
        payment.setQrCode(qrCode);
        payment = paymentRepository.save(payment);

        // Chuyển đổi thành response và trả về
        return modelMapper.map(payment, PaymentResponse.class);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Override
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + id));

        return modelMapper.map(payment, PaymentResponse.class);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Override
    public List<PaymentResponse> getPaymentsByBookingId(Long bookingId) {
        List<Payment> payments = paymentRepository.findByBookingId(bookingId);
        return payments.stream()
                .map(payment -> modelMapper.map(payment, PaymentResponse.class))
                .toList();
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    @Override
    public PaymentResponse updatePaymentStatus(Long id, String status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + id));

        String oldStatus = payment.getPaymentStatus();
        payment.setPaymentStatus(status);

        if (status.equals("Đã thanh toán")) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        payment = paymentRepository.save(payment);

        // Tạo lịch sử thanh toán
        createPaymentHistory(payment, "Cập nhật trạng thái từ " + oldStatus + " sang " + status, "update");

        return modelMapper.map(payment, PaymentResponse.class);
    }

    @Transactional
    @Override
    public PaymentResponse processPaymentCallback(PaymentCallbackRequest request) {
        // Kiểm tra tính xác thực của callback
        if (!verifyPaymentCallback(request)) {
            throw new RuntimeException("Callback không hợp lệ");
        }

        // Tìm payment dựa trên transaction ID
        Payment payment = paymentRepository.findByTransactionId(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với transaction id: " + request.getTransactionId()));

        // Cập nhật trạng thái payment
        String oldStatus = payment.getPaymentStatus();
        String newStatus;

        if ("success".equalsIgnoreCase(request.getStatus()) || "00".equals(request.getStatus())) {
            newStatus = "Đã thanh toán";
            payment.setPaymentDate(LocalDateTime.now());
        } else {
            newStatus = "Đã hủy";
        }

        payment.setPaymentStatus(newStatus);
        payment.setGatewayResponse(request.getMessage());

        payment = paymentRepository.save(payment);

        // Tạo lịch sử thanh toán
        createPaymentHistory(payment, "Callback từ cổng thanh toán: " + request.getMessage(), "callback");

        return modelMapper.map(payment, PaymentResponse.class);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public PaymentResponse refundPayment(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + paymentId));

        if (!"Đã thanh toán".equals(payment.getPaymentStatus())) {
            throw new RuntimeException("Chỉ có thể hoàn tiền cho thanh toán đã hoàn tất");
        }

        // Thực hiện hoàn tiền với cổng thanh toán
        // Đây là phần phụ thuộc vào cổng thanh toán cụ thể
        boolean refundSuccess = processRefundWithGateway(payment, request.getAmount(), request.getReason());

        if (refundSuccess) {
            // Cập nhật trạng thái payment
            payment.setPaymentStatus("Đã hoàn tiền");
//            payment.setRefundAmount(request.getAmount());
//            payment.setRefundReason(request.getReason());
//            payment.setRefundDate(LocalDateTime.now());

            payment = paymentRepository.save(payment);

            // Tạo lịch sử thanh toán
            createPaymentHistory(payment, "Hoàn tiền: " + request.getAmount() + " - Lý do: " + request.getReason(), "refund");

            return modelMapper.map(payment, PaymentResponse.class);
        } else {
            throw new RuntimeException("Hoàn tiền thất bại với cổng thanh toán");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + id));

        paymentRepository.delete(payment);
    }

    @Override
    public String generateQRCode(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + paymentId));

        // Thực hiện gọi API cổng thanh toán để tạo QR code
        // Đây là phần phụ thuộc vào cổng thanh toán cụ thể

        // Lấy gateway từ payment
        String gateway = payment.getGateway();

        // Dựa vào gateway để tạo QR code
        if (gateway != null) {
            switch (gateway.toLowerCase()) {
                case "vnpay":
                    return createVNPayQRCode(payment);
                case "zalopay":
                    return createZaloPayQRCode(payment);
                case "momo":
                    return createMomoQRCode(payment);
                default:
                    // Gateway không được hỗ trợ, sử dụng QR mặc định
                    log.warn("Gateway không được hỗ trợ: {}", gateway);
            }
        }

        // Giả lập việc tạo QR code (mặc định)
        return "https://payment.example.com/pay?id=" + payment.getTransactionId();
    }

    @Override
    public boolean verifyPaymentCallback(PaymentCallbackRequest request) {
        // Kiểm tra các trường bắt buộc có tồn tại không
        if (request.getTransactionId() == null || request.getSignature() == null) {
            log.error("Missing required fields in payment callback");
            return false;
        }

        try {
            String receivedSignature = request.getSignature();

            // Tạo chuỗi dữ liệu để kiểm tra (thường là sự kết hợp của các trường quan trọng)
            String dataToSign = createDataToSign(request);

            // Tính toán chữ ký dựa trên thuật toán và khóa bí mật được sử dụng
            String calculatedSignature = calculateSignature(dataToSign);

            // So sánh chữ ký đã tính toán với chữ ký nhận được
            boolean isValid = calculatedSignature != null && calculatedSignature.equals(receivedSignature);

            if (!isValid) {
                log.warn("Signature verification failed. Expected: {}, Received: {}", calculatedSignature, receivedSignature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying payment callback", e);
            return false;
        }
    }

    @Override
    public boolean checkPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + paymentId));

        // Nếu payment đã thanh toán, trả về true
        if ("Đã thanh toán".equals(payment.getPaymentStatus())) {
            return true;
        }

        // Nếu payment đã hết hạn, trả về false
        if (payment.getQrExpiryTime().isBefore(LocalDateTime.now())) {
            updatePaymentStatus(paymentId, "Đã hết hạn");
            return false;
        }

        // Nếu chưa thanh toán, kiểm tra với cổng thanh toán
        // Đây là phần phụ thuộc vào cổng thanh toán cụ thể
        String gateway = payment.getGateway();
        boolean isPaid = false;

        // Dựa vào gateway để kiểm tra
        if (gateway != null) {
            switch (gateway.toLowerCase()) {
                case "vnpay":
                    isPaid = checkVNPayStatus(payment);
                    break;
                case "zalopay":
                    isPaid = checkZaloPayStatus(payment);
                    break;
                case "momo":
                    isPaid = checkMomoStatus(payment);
                    break;
                default:
                    log.warn("Gateway không được hỗ trợ để kiểm tra: {}", gateway);
            }
        }

        // Cập nhật trạng thái nếu đã thanh toán
        if (isPaid) {
            updatePaymentStatus(paymentId, "Đã thanh toán");
            return true;
        }

        return false;
    }

    // Helper methods
    private String generateTransactionId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private void createPaymentHistory(Payment payment, String message, String action) {
        PaymentHistory history = new PaymentHistory();
        history.setPayment(payment);
        history.setStatus(payment.getPaymentStatus());
        history.setDescription(message);
//        history.setAction(action);
//        history.setCreatedAt(LocalDateTime.now());

        paymentHistoryRepository.save(history);
    }

    // Phương thức để tạo chuỗi dữ liệu cần ký
    private String createDataToSign(PaymentCallbackRequest request) {
        // Cách tạo chuỗi dữ liệu phụ thuộc vào quy định của cổng thanh toán
        // Ví dụ: kết hợp transactionId và status
        return request.getTransactionId() + "|" + request.getStatus() + "|" + request.getMessage();
    }

    // Phương thức để tính toán chữ ký
    private String calculateSignature(String data) {
        try {
            // Lấy khóa bí mật từ cấu hình
            String secretKey = paymentConfig.getSecretKey();

            // Kiểm tra xem secret key có tồn tại không
            if (secretKey == null || secretKey.trim().isEmpty()) {
                log.error("Secret key is missing in configuration");
                return null;
            }

            // Tạo HMAC với thuật toán SHA-256
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);

            // Tính toán chữ ký
            byte[] hmacBytes = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Chuyển đổi sang Base64
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            log.error("Error calculating signature", e);
            return null;
        }
    }

    // Phương thức cho việc hoàn tiền
    private boolean processRefundWithGateway(Payment payment, double amount, String reason) {
        // Đây là phần phụ thuộc vào cổng thanh toán cụ thể
        String gateway = payment.getGateway();

        if (gateway != null) {
            switch (gateway.toLowerCase()) {
                case "vnpay":
                    return processVNPayRefund(payment, amount, reason);
                case "zalopay":
                    return processZaloPayRefund(payment, amount, reason);
                case "momo":
                    return processMomoRefund(payment, amount, reason);
                default:
                    log.warn("Gateway không được hỗ trợ để hoàn tiền: {}", gateway);
            }
        }

        // Giả lập hoàn tiền thành công (chỉ cho mục đích phát triển)
        log.info("Giả lập hoàn tiền thành công cho payment: {}", payment.getId());
        return true;
    }

    // Các phương thức gọi API cổng thanh toán cụ thể

    // VNPay
    private String createVNPayQRCode(Payment payment) {
        // Implement logic to generate VNPay QR code
        // Đây chỉ là mã giả
        log.info("Tạo QR code VNPay cho payment: {}", payment.getId());
        return "vnpay://pay?transactionId=" + payment.getTransactionId()
                + "&amount=" + payment.getAmount()
                + "&description=" + "Thanh toán booking " + payment.getBooking().getId();
    }

    private boolean checkVNPayStatus(Payment payment) {
        // Implement logic to check VNPay payment status
        // Đây chỉ là mã giả
        log.info("Kiểm tra trạng thái VNPay cho payment: {}", payment.getId());
        return false; // Giả sử chưa thanh toán
    }

    private boolean processVNPayRefund(Payment payment, double amount, String reason) {
        // Implement logic for VNPay refund
        // Đây chỉ là mã giả
        log.info("Thực hiện hoàn tiền VNPay cho payment: {}", payment.getId());
        return true; // Giả sử hoàn tiền thành công
    }

    // ZaloPay
    private String createZaloPayQRCode(Payment payment) {
        // Implement logic to generate ZaloPay QR code
        // Đây chỉ là mã giả
        log.info("Tạo QR code ZaloPay cho payment: {}", payment.getId());
        return "zalopay://pay?transactionId=" + payment.getTransactionId()
                + "&amount=" + payment.getAmount();
    }

    private boolean checkZaloPayStatus(Payment payment) {
        // Implement logic to check ZaloPay payment status
        // Đây chỉ là mã giả
        log.info("Kiểm tra trạng thái ZaloPay cho payment: {}", payment.getId());
        return false; // Giả sử chưa thanh toán
    }

    private boolean processZaloPayRefund(Payment payment, double amount, String reason) {
        // Implement logic for ZaloPay refund
        // Đây chỉ là mã giả
        log.info("Thực hiện hoàn tiền ZaloPay cho payment: {}", payment.getId());
        return true; // Giả sử hoàn tiền thành công
    }

    // Momo
    private String createMomoQRCode(Payment payment) {
        // Implement logic to generate Momo QR code
        // Đây chỉ là mã giả
        log.info("Tạo QR code Momo cho payment: {}", payment.getId());
        return "momo://pay?transactionId=" + payment.getTransactionId()
                + "&amount=" + payment.getAmount();
    }

    private boolean checkMomoStatus(Payment payment) {
        // Implement logic to check Momo payment status
        // Đây chỉ là mã giả
        log.info("Kiểm tra trạng thái Momo cho payment: {}", payment.getId());
        return false; // Giả sử chưa thanh toán
    }

    private boolean processMomoRefund(Payment payment, double amount, String reason) {
        // Implement logic for Momo refund
        // Đây chỉ là mã giả
        log.info("Thực hiện hoàn tiền Momo cho payment: {}", payment.getId());
        return true; // Giả sử hoàn tiền thành công
    }
}