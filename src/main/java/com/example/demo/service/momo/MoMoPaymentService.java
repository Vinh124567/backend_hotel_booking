package com.example.demo.service.momo;
import com.example.demo.config.PaymentConfig;
import com.example.demo.dto.payment.MoMoPaymentResponse;
import com.example.demo.dto.payment.PaymentCallbackRequest;
import com.example.demo.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MoMoPaymentService {
    private static final Logger log = LoggerFactory.getLogger(MoMoPaymentService.class);

    private final PaymentConfig paymentConfig;
    private final RestTemplate restTemplate;

    public MoMoPaymentResponse createPaymentRequest(Payment payment) throws Exception {
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
        String amount = payment.getAmount().toBigInteger().toString();

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

        String signature = hmacSHA256(rawData, secretKey);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("accessKey", accessKey);
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                paymentConfig.getMomoApiUrl(),
                entity,
                Map.class
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new RuntimeException("MoMo API trả về response null");
        }

        Integer resultCode = (Integer) responseBody.get("resultCode");
        String message = (String) responseBody.get("message");

        if (resultCode == null || resultCode != 0) {
            String errorMsg = String.format("MoMo API Error - ResultCode: %s, Message: %s", resultCode, message);
            throw new RuntimeException(errorMsg);
        }

        MoMoPaymentResponse momoResponse = new MoMoPaymentResponse();
        momoResponse.setResultCode(resultCode);
        momoResponse.setMessage(message);
        momoResponse.setPayUrl((String) responseBody.get("payUrl"));
        momoResponse.setQrCodeUrl((String) responseBody.get("qrCodeUrl"));
        momoResponse.setSignature((String) responseBody.get("signature"));

        return momoResponse;
    }

    public boolean checkPaymentStatus(Payment payment) {
        try {
            String partnerCode = paymentConfig.getMomoPartnerCode();
            String accessKey = paymentConfig.getMomoAccessKey();
            String secretKey = paymentConfig.getMomoSecretKey();
            String requestId = generateRequestId();
            String orderId = payment.getOrderId();
            String lang = paymentConfig.getMomoLang();

            String rawData = "accessKey=" + accessKey +
                    "&orderId=" + orderId +
                    "&partnerCode=" + partnerCode +
                    "&requestId=" + requestId;

            String signature = hmacSHA256(rawData, secretKey);

            Map<String, Object> queryRequest = new LinkedHashMap<>();
            queryRequest.put("partnerCode", partnerCode);
            queryRequest.put("accessKey", accessKey);
            queryRequest.put("requestId", requestId);
            queryRequest.put("orderId", orderId);
            queryRequest.put("lang", lang);
            queryRequest.put("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(queryRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    paymentConfig.getMomoQueryUrl(),
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                Integer resultCode = (Integer) responseBody.get("resultCode");
                Object transIdObj = responseBody.get("transId");

                if (resultCode != null && resultCode == 0) {
                    if (transIdObj != null) {
                        String transId = convertTransIdToString(transIdObj);
                        if (transId != null && !transId.isEmpty() && !"0".equals(transId)) {
                            payment.setTransactionId(transId);
                        }
                    }
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            log.error("Error querying MoMo payment status for orderId: " + payment.getOrderId(), e);
            return false;
        }
    }

    public boolean verifyPaymentCallback(PaymentCallbackRequest request) {
        try {
            if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
                return false;
            }

            if (request.getResultCode() == null) {
                return false;
            }

            if (request.getAmount() == null || request.getAmount() <= 0) {
                return false;
            }

            if (!paymentConfig.getMomoPartnerCode().equals(request.getPartnerCode())) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Lỗi verify MoMo callback", e);
            return false;
        }
    }

    public String generateTransactionId() {
        return "MOMO_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis();
    }

    public String generateRequestId() {
        return "REQ_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Hex.encodeHexString(hash);
    }

    private String convertTransIdToString(Object transIdObj) {
        if (transIdObj instanceof String) {
            return (String) transIdObj;
        } else if (transIdObj instanceof Integer) {
            Integer transIdInt = (Integer) transIdObj;
            return transIdInt != 0 ? String.valueOf(transIdInt) : null;
        } else if (transIdObj != null) {
            return String.valueOf(transIdObj);
        }
        return null;
    }
}