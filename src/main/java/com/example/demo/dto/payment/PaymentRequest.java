package com.example.demo.dto.payment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Long bookingId;
    private BigDecimal amount;
    private String paymentMethod;
    private String gateway;
    private String redirectUrl;
    private String callbackUrl;
    private String notes;
}

