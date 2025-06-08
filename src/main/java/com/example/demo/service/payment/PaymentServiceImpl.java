package com.example.demo.service.payment;

import com.example.demo.config.PaymentConfig;
import com.example.demo.dto.payment.PaymentCallbackRequest;
import com.example.demo.dto.payment.PaymentRequest;
import com.example.demo.dto.payment.PaymentResponse;
import com.example.demo.dto.payment.PaymentStatusResponse;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentHistoryRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.booking.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    private final BookingService bookingService; // ← ✅ ADD THIS DEPENDENCY

    private final PaymentConfig paymentConfig;
    private final RestTemplate restTemplate;

    @Transactional
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // Validate booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại với id: " + request.getBookingId()));

        // Create payment entity
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setGateway(request.getGateway());
        payment.setPaymentStatus("Chờ thanh toán");
        payment.setNotes(request.getNotes());

        // Generate MoMo specific IDs
        payment.setTransactionId(generateTransactionId());
        payment.setOrderId(generateOrderId());
        payment.setRequestId(generateRequestId());
        payment.setPartnerCode(paymentConfig.getMomoPartnerCode());

        // Set expiry time (15 minutes)
        payment.setQrExpiryTime(LocalDateTime.now().plusMinutes(15));

        // Set URLs
        payment.setCallbackUrl(request.getCallbackUrl() != null ?
                request.getCallbackUrl() : paymentConfig.getMomoCallbackUrl());
        payment.setRedirectUrl(request.getRedirectUrl() != null ?
                request.getRedirectUrl() : paymentConfig.getMomoRedirectUrl());

        // Save payment to get ID
        payment = paymentRepository.save(payment);

        // Create MoMo payment request - GỌI API THỰC SỰ
        try {
            MoMoPaymentResponse momoResponse = createMoMoPaymentRequest(payment);

            // Update payment với response từ MoMo
            payment.setQrCode(momoResponse.getQrCodeUrl());
            payment.setPaymentUrl(momoResponse.getPayUrl());
            payment.setSignature(momoResponse.getSignature());

            payment = paymentRepository.save(payment);

            log.info("✅ Tạo MoMo payment thành công - PaymentId: {}, OrderId: {}",
                    payment.getId(), payment.getOrderId());

        } catch (Exception e) {
            log.error("❌ Lỗi tạo MoMo payment request cho payment: " + payment.getId(), e);
            throw new RuntimeException("Không thể tạo thanh toán MoMo: " + e.getMessage());
        }

        // Create payment history
        paymentHistoryRepository.save(
                PaymentHistory.createRecord(payment, "Khởi tạo thanh toán MoMo")
        );

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

        log.info("🔍 Checking payment status for paymentId: {}, orderId: {}", paymentId, payment.getOrderId());

        // ✅ Check local status first
        if (payment.isPaid()) {
            log.info("✅ Payment already marked as paid locally");
            return PaymentStatusResponse.success(paymentId);
        }

        // ✅ Check if expired
        if (payment.isExpired()) {
            log.info("⏰ Payment expired locally");
            updatePaymentStatus(paymentId, "Đã hết hạn");
            return PaymentStatusResponse.expired(paymentId);
        }

        // ✅ Query MoMo API for real status
        try {
            boolean isPaidAtMoMo = checkMoMoPaymentStatus(payment);
            if (isPaidAtMoMo) {
                log.info("🎉 MoMo confirms payment is successful - updating local status");
                updatePaymentStatus(paymentId, "Đã thanh toán");
                return PaymentStatusResponse.success(paymentId);
            }
        } catch (Exception e) {
            log.error("❌ Error checking MoMo status for payment: " + paymentId, e);
        }

        // ✅ Still pending
        log.info("⏳ Payment still pending");
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

            // 🎯 ✅ AUTO CONFIRM BOOKING when manually set to "Đã thanh toán"
            try {
                Long bookingId = payment.getBooking().getId();
                log.info("🏨 Manual update: Auto-confirming booking: {} after payment marked as paid", bookingId);

                bookingService.confirmBooking(bookingId);
                log.info("✅ Booking auto-confirmed after manual payment update: {}", bookingId);

            } catch (Exception e) {
                log.error("❌ Failed to confirm booking after manual payment update: {}", payment.getId(), e);

                // Don't throw - payment update was successful
                paymentHistoryRepository.save(
                        PaymentHistory.createRecord(payment,
                                "WARNING: Manual payment update successful but booking confirmation failed: " + e.getMessage())
                );
            }
        }

        payment = paymentRepository.save(payment);

        // Create payment history
        paymentHistoryRepository.save(
                PaymentHistory.updateRecord(payment,
                        "Manual status update from " + oldStatus + " to " + status)
        );

        return modelMapper.map(payment, PaymentResponse.class);
    }

    @Transactional
    @Override
    public PaymentResponse processPaymentCallback(PaymentCallbackRequest request) {
        log.info("🎯 Processing MoMo callback for orderId: {}, resultCode: {}",
                request.getOrderId(), request.getResultCode());

        // Verify callback
        if (!verifyPaymentCallback(request)) {
            log.error("❌ MoMo callback không hợp lệ cho orderId: {}", request.getOrderId());
            throw new RuntimeException("MoMo callback không hợp lệ");
        }

        // Find payment
        Payment payment = findPaymentByCallback(request);

        // Update payment status
        String newPaymentStatus = request.isSuccess() ? "Đã thanh toán" : "Đã hủy";
        payment.setPaymentStatus(newPaymentStatus);
        payment.setGatewayResponse(request.getMessage());

        if (request.isSuccess()) {
            payment.setPaymentDate(LocalDateTime.now());
            if (request.getTransId() != null && !request.getTransId().isEmpty()) {
                payment.setTransactionId(request.getTransId());
            }
        }

        payment = paymentRepository.save(payment);

        // Create payment history
        paymentHistoryRepository.save(
                PaymentHistory.callbackRecord(payment,
                        "MoMo callback: " + request.getMessage(),
                        request.toString())
        );

        log.info("✅ Payment status updated to: {} for paymentId: {}", newPaymentStatus, payment.getId());

        // 🎯 ✅ AUTO CONFIRM BOOKING when payment success
        if (request.isSuccess()) {
            try {
                Long bookingId = payment.getBooking().getId();
                log.info("🏨 Auto-confirming booking: {} after payment success", bookingId);

                bookingService.confirmBooking(bookingId);
                log.info("✅ Booking auto-confirmed successfully: {}", bookingId);

            } catch (Exception e) {
                log.error("❌ CRITICAL: Payment successful but booking confirmation failed for payment: {}",
                        payment.getId(), e);

                // 🚨 IMPORTANT: DON'T throw exception here - payment was successful
                // Create error record for manual intervention
                paymentHistoryRepository.save(
                        PaymentHistory.createRecord(payment,
                                "ERROR: Payment successful but booking confirmation failed: " + e.getMessage())
                );
            }
        }

        return modelMapper.map(payment, PaymentResponse.class);
    }

    @Override
    public String generateQRCode(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại"));

        String qrCode = payment.getQrCode();
        log.info("🔍 Trả về QR code cho payment {}: {}", paymentId, qrCode);

        return qrCode;
    }

    /**
     * ĐỒ ÁN TỐT NGHIỆP: SIMPLE CALLBACK VERIFICATION
     *
     * Trong production thật sẽ verify HMAC-SHA256 signature từ MoMo
     * để đảm bảo callback không bị giả mạo. Tuy nhiên, cho mục đích
     * đồ án demo, chúng ta sử dụng basic validation để tập trung vào
     * core business logic.
     *
     * Production implementation sẽ bao gồm:
     * - HMAC-SHA256 signature verification với secret key
     * - Timestamp validation để tránh replay attacks
     * - Request deduplication
     * - Rate limiting
     */
    @Override
    public boolean verifyPaymentCallback(PaymentCallbackRequest request) {
        try {
            log.info("🎓 STUDENT PROJECT MODE: Processing MoMo callback");
            log.info("📋 OrderId: {}, Amount: {}, ResultCode: {}, Message: {}",
                    request.getOrderId(), request.getAmount(),
                    request.getResultCode(), request.getMessage());

            // Basic validation - check required fields
            if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
                log.error("❌ OrderId is null or empty");
                return false;
            }

            if (request.getResultCode() == null) {
                log.error("❌ ResultCode is null");
                return false;
            }

            if (request.getAmount() == null || request.getAmount() <= 0) {
                log.error("❌ Amount is invalid: {}", request.getAmount());
                return false;
            }

            // Check if partnerCode matches (basic security)
            if (!paymentConfig.getMomoPartnerCode().equals(request.getPartnerCode())) {
                log.error("❌ PartnerCode mismatch. Expected: {}, Received: {}",
                        paymentConfig.getMomoPartnerCode(), request.getPartnerCode());
                return false;
            }

            log.info("✅ Callback validation passed - OrderId: {}, ResultCode: {}",
                    request.getOrderId(), request.getResultCode());

            return true;

        } catch (Exception e) {
            log.error("❌ Lỗi verify MoMo callback", e);
            return false;
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

    // ========== MoMo API Integration Methods ==========

    /**
     * GỌI MOMO API ĐỂ TẠO PAYMENT REQUEST
     */
    private MoMoPaymentResponse createMoMoPaymentRequest(Payment payment) throws Exception {
        // Các giá trị cấu hình và tham số MoMo
        String partnerCode = paymentConfig.getMomoPartnerCode();
        String accessKey = paymentConfig.getMomoAccessKey();
        String secretKey = paymentConfig.getMomoSecretKey();
        String requestId = payment.getRequestId();
        String orderId = payment.getOrderId();
        String orderInfo = paymentConfig.getMomoOrderInfo();
        String redirectUrl = payment.getRedirectUrl();
        String ipnUrl = payment.getCallbackUrl();
        String requestType = paymentConfig.getMomoRequestType();
        String extraData = "";
        String lang = paymentConfig.getMomoLang();
        String amount = payment.getAmount().toBigInteger().toString(); // Đảm bảo không có .0

        // ✅ Tạo rawData đúng thứ tự alphabet
        String rawData = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        // ✅ Tạo signature từ rawData và secretKey
        String signature = hmacSHA256(rawData, secretKey);

        // ✅ Gửi log debug
        log.info("🔐 MoMo rawData: {}", rawData);
        log.info("🔑 MoMo signature: {}", signature);

        // ✅ Tạo request body gửi lên MoMo
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("accessKey", accessKey); // Bắt buộc phải có
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", redirectUrl);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("requestType", requestType);
        requestBody.put("extraData", extraData);
        requestBody.put("lang", lang);
        requestBody.put("signature", signature);

        log.info("📤 MoMo Request JSON: {}", new ObjectMapper().writeValueAsString(requestBody));

        // Gửi request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    paymentConfig.getMomoApiUrl(),
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            log.info("📱 MoMo API Response: {}", responseBody);

            if (responseBody == null) {
                throw new RuntimeException("MoMo API trả về response null");
            }

            Integer resultCode = (Integer) responseBody.get("resultCode");
            String message = (String) responseBody.get("message");

            if (resultCode == null || resultCode != 0) {
                String errorMsg = String.format("MoMo API Error - ResultCode: %s, Message: %s",
                        resultCode, message);
                log.error("❌ {}", errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // Parse response
            MoMoPaymentResponse momoResponse = new MoMoPaymentResponse();
            momoResponse.setResultCode(resultCode);
            momoResponse.setMessage(message);
            momoResponse.setPayUrl((String) responseBody.get("payUrl"));
            momoResponse.setQrCodeUrl((String) responseBody.get("qrCodeUrl"));
            momoResponse.setSignature((String) responseBody.get("signature"));

            log.info("✅ MoMo payment tạo thành công - OrderId: {}, PayUrl: {}",
                    payment.getOrderId(), momoResponse.getPayUrl());

            return momoResponse;

        } catch (Exception e) {
            log.error("❌ Lỗi gọi MoMo API cho payment: " + payment.getId(), e);
            throw new RuntimeException("Không thể kết nối MoMo API: " + e.getMessage());
        }
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(hash);
    }


    private boolean checkMoMoPaymentStatus(Payment payment) {
        try {
            log.info("🔍 Querying MoMo API for payment status - OrderId: {}", payment.getOrderId());

            // Build MoMo Query request parameters
            String partnerCode = paymentConfig.getMomoPartnerCode();
            String accessKey = paymentConfig.getMomoAccessKey();
            String secretKey = paymentConfig.getMomoSecretKey();
            String requestId = generateRequestId(); // Generate new requestId for query
            String orderId = payment.getOrderId();
            String lang = paymentConfig.getMomoLang();

            // ✅ Create rawData for Query API signature (alphabetical order)
            String rawData = "accessKey=" + accessKey +
                    "&orderId=" + orderId +
                    "&partnerCode=" + partnerCode +
                    "&requestId=" + requestId;

            // ✅ Generate signature
            String signature = hmacSHA256(rawData, secretKey);

            log.info("🔐 Query rawData: {}", rawData);
            log.info("🔑 Query signature: {}", signature);

            // ✅ Build request body for Query API
            Map<String, Object> queryRequest = new LinkedHashMap<>();
            queryRequest.put("partnerCode", partnerCode);
            queryRequest.put("accessKey", accessKey);
            queryRequest.put("requestId", requestId);
            queryRequest.put("orderId", orderId);
            queryRequest.put("lang", lang);
            queryRequest.put("signature", signature);

            log.info("📤 MoMo Query Request: {}", new ObjectMapper().writeValueAsString(queryRequest));

            // ✅ Call MoMo Query API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(queryRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    paymentConfig.getMomoQueryUrl(), // https://test-payment.momo.vn/v2/gateway/api/query
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            log.info("📱 MoMo Query Response: {}", responseBody);

            if (responseBody != null) {
                Integer resultCode = (Integer) responseBody.get("resultCode");
                String message = (String) responseBody.get("message");

                // 🎯 FIX: Handle transId properly - can be Integer or String
                Object transIdObj = responseBody.get("transId");
                String transId = null;

                if (transIdObj != null) {
                    if (transIdObj instanceof String) {
                        transId = (String) transIdObj;
                    } else if (transIdObj instanceof Integer) {
                        Integer transIdInt = (Integer) transIdObj;
                        // Only convert to string if it's not 0 (0 means no transaction)
                        if (transIdInt != 0) {
                            transId = String.valueOf(transIdInt);
                        }
                    } else {
                        // Handle other types (Long, etc.)
                        transId = String.valueOf(transIdObj);
                    }
                }

                log.info("📊 Query Result - ResultCode: {}, Message: {}, TransId: {}",
                        resultCode, message, transId);

                // ✅ ResultCode = 0 means payment successful
                if (resultCode != null && resultCode == 0) {
                    log.info("✅ MoMo confirms payment SUCCESS for orderId: {}", orderId);

                    // Update transactionId if available and not empty/zero
                    if (transId != null && !transId.isEmpty() && !"0".equals(transId)) {
                        payment.setTransactionId(transId);
                        paymentRepository.save(payment);
                        log.info("💳 Updated transaction ID: {}", transId);
                    }

                    return true;
                } else {
                    log.info("⏳ MoMo payment not successful - ResultCode: {}, Message: {}",
                            resultCode, message);
                    return false;
                }
            }

            log.warn("⚠️ MoMo Query API returned empty response");
            return false;

        } catch (Exception e) {
            log.error("❌ Error querying MoMo payment status for orderId: " + payment.getOrderId(), e);
            return false;
        }
    }


    // ========== Signature Calculation Methods ==========

    private String createMoMoSignature(Payment payment) {
        // Debug trước khi tạo signature
        debugSignatureData(payment);

        // MoMo yêu cầu thứ tự tham số chính xác theo alphabet
        // Thứ tự PHẢI là: accessKey -> amount -> extraData -> ipnUrl -> orderId -> orderInfo -> partnerCode -> redirectUrl -> requestId -> requestType
        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                paymentConfig.getMomoAccessKey(),
                payment.getAmount(),
                "", // extraData luôn là empty string cho captureWallet
                payment.getCallbackUrl(),
                payment.getOrderId(),
                paymentConfig.getMomoOrderInfo(),
                payment.getPartnerCode(),
                payment.getRedirectUrl(),
                payment.getRequestId(),
                paymentConfig.getMomoRequestType()
        );

        log.info("🔐 MoMo signature raw data: {}", rawSignature);
        String signature = calculateHMacSHA256(rawSignature, paymentConfig.getMomoSecretKey());
        log.info("🔐 Generated signature: {}", signature);
        return signature;
    }

// ========== Alternative: Auto-sort parameters ==========

    private String createMoMoSignatureAutoSort(Payment payment) {
        // Tạo map các tham số
        Map<String, String> params = new LinkedHashMap<>();
        params.put("accessKey", paymentConfig.getMomoAccessKey());
        params.put("amount", payment.getAmount().toString());
        params.put("extraData", ""); // Luôn empty cho captureWallet
        params.put("ipnUrl", payment.getCallbackUrl());
        params.put("orderId", payment.getOrderId());
        params.put("orderInfo", paymentConfig.getMomoOrderInfo());
        params.put("partnerCode", payment.getPartnerCode());
        params.put("redirectUrl", payment.getRedirectUrl());
        params.put("requestId", payment.getRequestId());
        params.put("requestType", paymentConfig.getMomoRequestType());

        // Sắp xếp theo alphabet và tạo chuỗi signature
        String rawSignature = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        log.info("🔐 MoMo signature raw data (auto-sorted): {}", rawSignature);
        return calculateHMacSHA256(rawSignature, paymentConfig.getMomoSecretKey());
    }

// ========== Debugging Method ==========

    private void debugSignatureData(Payment payment) {
        log.info("🔍 === MoMo Signature Debug ===");
        log.info("accessKey: {}", paymentConfig.getMomoAccessKey());
        log.info("amount: {}", payment.getAmount());
        log.info("extraData: [empty]");
        log.info("ipnUrl: {}", payment.getCallbackUrl());
        log.info("orderId: {}", payment.getOrderId());
        log.info("orderInfo: {}", paymentConfig.getMomoOrderInfo());
        log.info("partnerCode: {}", payment.getPartnerCode());
        log.info("redirectUrl: {}", payment.getRedirectUrl());
        log.info("requestId: {}", payment.getRequestId());
        log.info("requestType: {}", paymentConfig.getMomoRequestType());
        log.info("secretKey: {} (length: {})",
                paymentConfig.getMomoSecretKey().substring(0, 5) + "***",
                paymentConfig.getMomoSecretKey().length());
        log.info("🔍 === End Debug ===");
    }

    // ========== Helper Methods ==========

    private Payment findPaymentByCallback(PaymentCallbackRequest request) {
        return paymentRepository.findByOrderId(request.getOrderId())
                .or(() -> paymentRepository.findByTransactionId(request.getTransactionId()))
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại cho orderId: " + request.getOrderId()));
    }

    private String generateTransactionId() {
        return "MOMO_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis();
    }

    private String generateRequestId() {
        return "REQ_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String calculateHMacSHA256(String data, String secretKey) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] hmacBytes = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // MoMo yêu cầu hexadecimal lowercase, KHÔNG phải Base64!
            StringBuilder result = new StringBuilder();
            for (byte b : hmacBytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();

        } catch (Exception e) {
            log.error("❌ Lỗi tính toán HMAC SHA256", e);
            throw new RuntimeException("Không thể tính toán signature", e);
        }
    }

    // ========== Response DTO ==========
    public static class MoMoPaymentResponse {
        private Integer resultCode;
        private String message;
        private String payUrl;
        private String qrCodeUrl;
        private String signature;

        // Getters and setters
        public Integer getResultCode() { return resultCode; }
        public void setResultCode(Integer resultCode) { this.resultCode = resultCode; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPayUrl() { return payUrl; }
        public void setPayUrl(String payUrl) { this.payUrl = payUrl; }

        public String getQrCodeUrl() { return qrCodeUrl; }
        public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
    }

    @Transactional
    public PaymentResponse simulatePaymentSuccess(Long paymentId) {
        log.info("🧪 TEST MODE: Simulating payment success for payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment không tồn tại với id: " + paymentId));

        // Update payment status
        String oldStatus = payment.getPaymentStatus();
        payment.setPaymentStatus("Đã thanh toán");
        payment.setPaymentDate(LocalDateTime.now());
        payment.setGatewayResponse("TEST: Simulated success");

        payment = paymentRepository.save(payment);

        // Create payment history
        paymentHistoryRepository.save(
                PaymentHistory.updateRecord(payment,
                        "TEST: Simulated success from " + oldStatus + " to Đã thanh toán")
        );

        // 🎯 ✅ AUTO CONFIRM BOOKING for test
        Long bookingId = payment.getBooking().getId();
        log.info("🏨 TEST: Auto-confirming booking: {} after simulated payment", bookingId);

        // ✅ Let GlobalException handle any errors - simple throw
        bookingService.confirmBooking(bookingId);
        log.info("✅ TEST: Booking auto-confirmed successfully: {}", bookingId);

        log.info("✅ Payment {} marked as successful (TEST MODE)", paymentId);
        return modelMapper.map(payment, PaymentResponse.class);
    }

}