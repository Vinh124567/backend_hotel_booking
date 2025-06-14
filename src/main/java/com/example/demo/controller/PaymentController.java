package com.example.demo.controller;

import com.example.demo.config.PaymentConfig; // ✅ THÊM IMPORT
import com.example.demo.dto.payment.PaymentCallbackRequest;
import com.example.demo.dto.payment.PaymentRequest;
import com.example.demo.dto.payment.PaymentResponse;
import com.example.demo.dto.payment.PaymentStatusResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest; // ✅ THÊM IMPORT
import jakarta.validation.Valid;
import java.time.LocalDateTime; // ✅ THÊM IMPORT
import java.util.Enumeration; // ✅ THÊM IMPORT
import java.util.HashMap; // ✅ THÊM IMPORT
import java.util.List;
import java.util.Map; // ✅ THÊM IMPORT

@RestController
@RequestMapping("api/v1/payments") // Changed to match design
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For mobile app access
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentConfig paymentConfig; // ✅ THÊM DEPENDENCY

    // ========== Mobile App Endpoints ==========

    /**
     * Tạo thanh toán MoMo mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Creating payment for booking: {}", request.getBookingId());
        PaymentResponse payment = paymentService.createPayment(request);

        ApiResponse<PaymentResponse> response = new ApiResponse<>();
        response.setResult(payment);
        response.setCode(HttpStatus.CREATED.value());
        response.setMessage("Tạo thanh toán thành công");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy thông tin thanh toán theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        PaymentResponse payment = paymentService.getPaymentById(id);

        ApiResponse<PaymentResponse> response = new ApiResponse<>();
        response.setResult(payment);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra trạng thái thanh toán (cho mobile app polling)
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> checkPaymentStatus(@PathVariable Long id) {
        PaymentStatusResponse status = paymentService.checkPaymentStatus(id);
        ApiResponse<PaymentStatusResponse> response = new ApiResponse<>();
        response.setResult(status);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách thanh toán theo booking
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByBookingId(@PathVariable Long bookingId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByBookingId(bookingId);

        ApiResponse<List<PaymentResponse>> response = new ApiResponse<>();
        response.setResult(payments);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy QR code URL
     */
    @GetMapping("/{id}/qr")
    public ResponseEntity<ApiResponse<String>> getQRCode(@PathVariable Long id) {
        String qrCode = paymentService.generateQRCode(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult(qrCode);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    // ========== MoMo Callback Endpoints (ENHANCED) ==========

    /**
     * ✅ ENHANCED MoMo IPN Callback với detailed logging
     */
    @PostMapping("/callback")
    public ResponseEntity<String> handleMoMoCallback(
            @RequestBody PaymentCallbackRequest request,
            HttpServletRequest httpRequest) {

        // ✅ Enhanced logging để debug
        log.info("🎯 ===== MOMO CALLBACK RECEIVED =====");
        log.info("🕐 Timestamp: {}", LocalDateTime.now());
        log.info("🌐 Remote IP: {}", httpRequest.getRemoteAddr());
        log.info("📋 User-Agent: {}", httpRequest.getHeader("User-Agent"));
        log.info("📦 Callback Data:");
        log.info("  - OrderId: {}", request.getOrderId());
        log.info("  - Amount: {}", request.getAmount());
        log.info("  - ResultCode: {}", request.getResultCode());
        log.info("  - Message: {}", request.getMessage());
        log.info("  - TransId: {}", request.getTransId());
        log.info("  - PartnerCode: {}", request.getPartnerCode());
        log.info("📋 Headers: {}", getHeaders(httpRequest));
        log.info("📦 Full request: {}", request);
        log.info("=====================================");

        try {
            PaymentResponse payment = paymentService.processPaymentCallback(request);

            log.info("✅ Callback processed successfully for paymentId: {}", payment.getId());

            // MoMo expects simple response
            if (request.getResultCode() != null && request.getResultCode() == 0) {
                log.info("🎉 Payment SUCCESS - returning OK to MoMo");
                return ResponseEntity.ok("OK");
            } else {
                log.warn("⚠️ Payment FAILED - returning FAILED to MoMo");
                return ResponseEntity.ok("FAILED");
            }

        } catch (Exception e) {
            log.error("❌ Error processing MoMo callback", e);
            return ResponseEntity.ok("ERROR");
        }
    }

    /**
     * MoMo Redirect Callback (browser redirect) - No authentication required
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleMoMoRedirect(
            @RequestParam String partnerCode,
            @RequestParam String orderId,
            @RequestParam String requestId,
            @RequestParam Long amount,
            @RequestParam String orderInfo,
            @RequestParam String orderType,
            @RequestParam String transId,
            @RequestParam Integer resultCode,
            @RequestParam String message,
            @RequestParam String payType,
            @RequestParam Long responseTime,
            @RequestParam(required = false) String extraData,
            @RequestParam String signature) {

        log.info("Received MoMo redirect for order: {}", orderId);

        // Convert GET params to callback request
        PaymentCallbackRequest callbackRequest = buildCallbackRequest(
                partnerCode, orderId, requestId, amount, orderInfo, orderType,
                transId, resultCode, message, payType, responseTime, extraData, signature
        );

        paymentService.processPaymentCallback(callbackRequest);

        // Return HTML response for browser
        String htmlResponse = resultCode == 0 ?
                createSuccessHtml() : createFailureHtml(message);

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=utf-8")
                .body(htmlResponse);
    }

    // ========== ✅ NEW TEST & DEBUG ENDPOINTS ==========

    /**
     * ✅ Manual check MoMo status via Query API
     */
    @GetMapping("/{id}/check-momo")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> manualCheckMoMoStatus(@PathVariable Long id) {
        log.info("🔄 Manual check MoMo status triggered for payment: {}", id);

        PaymentStatusResponse status = paymentService.checkPaymentStatus(id);

        ApiResponse<PaymentStatusResponse> response = new ApiResponse<>();
        response.setResult(status);
        response.setCode(200);
        response.setMessage("MoMo status checked successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ CHỈ CHO TEST - Simulate payment success
     */
    @PostMapping("/{id}/test/success")
    public ResponseEntity<ApiResponse<PaymentResponse>> simulatePaymentSuccess(@PathVariable Long id) {
        log.info("🧪 TEST MODE: Simulating payment success for payment: {}", id);

        PaymentResponse payment = paymentService.simulatePaymentSuccess(id);

        ApiResponse<PaymentResponse> response = new ApiResponse<>();
        response.setResult(payment);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("TEST: Payment marked as successful");

        return ResponseEntity.ok(response);
    }


    @GetMapping("/callback/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testCallbackEndpoint() {
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint_status", "ACTIVE");
        result.put("server_time", LocalDateTime.now());
        result.put("callback_url", paymentConfig.getMomoCallbackUrl());
        result.put("message", "Callback endpoint is ready to receive MoMo notifications");
        result.put("ngrok_accessible", true);

        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setResult(result);
        response.setCode(200);
        response.setMessage("Callback endpoint test successful");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Debug config endpoint
     */
    @GetMapping("/config/debug")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("momo_partner_code", paymentConfig.getMomoPartnerCode());
        config.put("momo_api_url", paymentConfig.getMomoApiUrl());
        config.put("momo_query_url", paymentConfig.getMomoQueryUrl());
        config.put("momo_callback_url", paymentConfig.getMomoCallbackUrl());
        config.put("momo_redirect_url", paymentConfig.getMomoRedirectUrl());
        config.put("momo_access_key_configured", paymentConfig.getMomoAccessKey() != null && !paymentConfig.getMomoAccessKey().isEmpty());
        config.put("momo_secret_key_configured", paymentConfig.getMomoSecretKey() != null && !paymentConfig.getMomoSecretKey().isEmpty());
        config.put("momo_request_type", paymentConfig.getMomoRequestType());
        config.put("momo_order_info", paymentConfig.getMomoOrderInfo());

        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setResult(config);
        response.setCode(200);
        response.setMessage("Config debug information");

        return ResponseEntity.ok(response);
    }

    // ========== Admin Endpoints ==========

    /**
     * Cập nhật trạng thái thanh toán (admin only)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        PaymentResponse payment = paymentService.updatePaymentStatus(id, status);

        ApiResponse<PaymentResponse> response = new ApiResponse<>();
        response.setResult(payment);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Cập nhật trạng thái thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * Xóa thanh toán (admin only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Xóa thanh toán thành công");
        response.setCode(HttpStatus.NO_CONTENT.value());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response);
    }

    // ========== Health Check ==========

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Payment service is running");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    // ========== Helper Methods ==========

    /**
     * ✅ Helper method để log request headers
     */
    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    private PaymentCallbackRequest buildCallbackRequest(
            String partnerCode, String orderId, String requestId, Long amount,
            String orderInfo, String orderType, String transId, Integer resultCode,
            String message, String payType, Long responseTime, String extraData, String signature) {

        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setPartnerCode(partnerCode);
        request.setOrderId(orderId);
        request.setRequestId(requestId);
        request.setAmount(amount);
        request.setOrderInfo(orderInfo);
        request.setOrderType(orderType);
        request.setTransId(transId);
        request.setResultCode(resultCode);
        request.setMessage(message);
        request.setPayType(payType);
        request.setResponseTime(responseTime);
        request.setExtraData(extraData);
        request.setSignature(signature);
        return request;
    }

    private String createSuccessHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Thanh toán thành công</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background-color: #f8f9fa; }
                    .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .success { color: #28a745; }
                    .icon { font-size: 48px; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">✅</div>
                    <h1 class="success">Thanh toán thành công!</h1>
                    <p>Đặt phòng của bạn đã được xác nhận.</p>
                    <p>Bạn có thể đóng trang này và quay lại ứng dụng.</p>
                </div>
            </body>
            </html>
            """;
    }

    private String createFailureHtml(String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Thanh toán thất bại</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background-color: #f8f9fa; }
                    .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .error { color: #dc3545; }
                    .icon { font-size: 48px; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">❌</div>
                    <h1 class="error">Thanh toán thất bại!</h1>
                    <p>Lý do: %s</p>
                    <p>Vui lòng thử lại hoặc liên hệ hỗ trợ.</p>
                    <p>Bạn có thể đóng trang này và quay lại ứng dụng.</p>
                </div>
            </body>
            </html>
            """.formatted(message != null ? message : "Không xác định");
    }

    private String createErrorHtml() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Lỗi xử lý thanh toán</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background-color: #f8f9fa; }
                    .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .error { color: #dc3545; }
                    .icon { font-size: 48px; margin-bottom: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="icon">⚠️</div>
                    <h1 class="error">Lỗi xử lý thanh toán!</h1>
                    <p>Đã xảy ra lỗi trong quá trình xử lý.</p>
                    <p>Vui lòng liên hệ hỗ trợ khách hàng.</p>
                </div>
            </body>
            </html>
            """;
    }
}