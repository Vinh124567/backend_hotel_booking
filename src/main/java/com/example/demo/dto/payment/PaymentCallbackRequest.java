package com.example.demo.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {
    private String transactionId;
    private String status;
    private String message;
    private String signature;
}