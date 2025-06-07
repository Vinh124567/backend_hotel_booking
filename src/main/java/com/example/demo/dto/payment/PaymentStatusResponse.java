package com.example.demo.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    private Long paymentId;
    private String status;
    private boolean isPaid;
    private boolean isExpired;
    private String message;

    public static PaymentStatusResponse success(Long paymentId) {
        return new PaymentStatusResponse(paymentId, "Đã thanh toán", true, false, "Thanh toán thành công");
    }

    public static PaymentStatusResponse pending(Long paymentId) {
        return new PaymentStatusResponse(paymentId, "Chờ thanh toán", false, false, "Đang chờ thanh toán");
    }

    public static PaymentStatusResponse expired(Long paymentId) {
        return new PaymentStatusResponse(paymentId, "Đã hết hạn", false, true, "Giao dịch đã hết hạn");
    }

    public static PaymentStatusResponse failed(Long paymentId, String message) {
        return new PaymentStatusResponse(paymentId, "Đã hủy", false, false, message);
    }
}