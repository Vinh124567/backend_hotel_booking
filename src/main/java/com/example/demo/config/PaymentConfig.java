package com.example.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment")
@Data
public class PaymentConfig {

    // General payment config
    private String secretKey;

    // MoMo configuration
    private MoMo momo = new MoMo();

    @Data
    public static class MoMo {
        // MoMo credentials
        private String partnerCode;
        private String accessKey;
        private String secretKey;

        // MoMo API URLs
        private String apiUrl = "https://test-payment.momo.vn/v2/gateway/api/create";
        private String queryUrl = "https://test-payment.momo.vn/v2/gateway/api/query";

        // Application URLs
        private String callbackUrl;
        private String redirectUrl;

        // Request settings
        private String requestType = "captureWallet";
        private String orderInfo = "Thanh toán đặt phòng khách sạn";
        private String lang = "vi";

        // QR code settings
        private int qrExpiryMinutes = 15;

        // Retry settings
        private int maxRetries = 3;
        private int retryDelaySeconds = 5;
    }

    // Getter methods for backward compatibility
    public String getMomoPartnerCode() {
        return momo.getPartnerCode();
    }

    public String getMomoAccessKey() {
        return momo.getAccessKey();
    }

    public String getMomoSecretKey() {
        return momo.getSecretKey();
    }

    public String getMomoCallbackUrl() {
        return momo.getCallbackUrl();
    }

    public String getMomoRedirectUrl() {
        return momo.getRedirectUrl();
    }

    public String getMomoApiUrl() {
        return momo.getApiUrl();
    }

    public String getMomoQueryUrl() {
        return momo.getQueryUrl();
    }

    public String getMomoRequestType() {
        return momo.getRequestType();
    }

    public String getMomoOrderInfo() {
        return momo.getOrderInfo();
    }

    public String getMomoLang() {
        return momo.getLang();
    }

    public int getMomoQrExpiryMinutes() {
        return momo.getQrExpiryMinutes();
    }

    public int getMomoMaxRetries() {
        return momo.getMaxRetries();
    }

    public int getMomoRetryDelaySeconds() {
        return momo.getRetryDelaySeconds();
    }

    // THÊM MỚI: Method để debug config loading
    public void logConfig() {
        System.out.println("=== PaymentConfig Debug ===");
        System.out.println("MoMo Partner Code: " + getMomoPartnerCode());
        System.out.println("MoMo Access Key: " + getMomoAccessKey());
        System.out.println("MoMo Secret Key: " + (getMomoSecretKey() != null ? "***configured***" : "NULL"));
        System.out.println("MoMo API URL: " + getMomoApiUrl());
        System.out.println("MoMo Callback URL: " + getMomoCallbackUrl());
        System.out.println("==========================");
    }
}