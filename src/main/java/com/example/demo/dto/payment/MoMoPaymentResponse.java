package com.example.demo.dto.payment;
import lombok.Data;

@Data
public class MoMoPaymentResponse {
    private Integer resultCode;
    private String message;
    private String payUrl;
    private String qrCodeUrl;
    private String signature;
}