package com.example.demo.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotNull(message = "Booking ID không được null")
    private Long bookingId;

    @NotNull(message = "Số tiền không được null")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;
    private String paymentMethod = "Ví điện tử";  // Default cho MoMo
    private String gateway = "momo";              // Default cho MoMo
    private String redirectUrl;
    private String callbackUrl;
    private String notes;
}
