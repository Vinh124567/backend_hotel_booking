package com.example.demo.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {
    // MoMo callback fields
    private String partnerCode;
    private String orderId;
    private String requestId;
    private Long amount;
    private String orderInfo;
    private String orderType;
    private String transId;  // MoMo transaction ID
    private Integer resultCode;  // 0 = success, other = failed
    private String message;
    private String payType;
    private Long responseTime;
    private String extraData;
    private String signature;

    // Legacy fields for compatibility
    private String transactionId;  // Map từ transId
    private String status;         // Map từ resultCode

    // Helper methods
    public String getTransactionId() {
        return transId != null ? transId : transactionId;
    }

    public String getStatus() {
        if (resultCode != null) {
            return resultCode == 0 ? "success" : "failed";
        }
        return status;
    }

    public boolean isSuccess() {
        return resultCode != null && resultCode == 0;
    }
}
