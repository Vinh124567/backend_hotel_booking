package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_history")
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "status")
    private String status;  // 'Chờ thanh toán', 'Đã thanh toán', 'Đã hủy', 'Hoàn tiền', 'Đã hết hạn'

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column(name = "description")
    private String description;

    // NEW: Thêm action để track loại thao tác
    @Column(name = "action")
    private String action;  // "create", "update", "callback", "timeout", "cancel"

    // NEW: Thêm response data từ MoMo nếu có
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }

    // Constructor helper cho việc tạo history record
    public PaymentHistory(Payment payment, String status, BigDecimal amount, String description, String action) {
        this.payment = payment;
        this.status = status;
        this.amount = amount;
        this.description = description;
        this.action = action;
        this.transactionDate = LocalDateTime.now();
    }

    // Static factory methods cho common use cases
    public static PaymentHistory createRecord(Payment payment, String description) {
        return new PaymentHistory(payment, payment.getPaymentStatus(), payment.getAmount(), description, "create");
    }

    public static PaymentHistory updateRecord(Payment payment, String description) {
        return new PaymentHistory(payment, payment.getPaymentStatus(), payment.getAmount(), description, "update");
    }

    public static PaymentHistory callbackRecord(Payment payment, String description, String gatewayResponse) {
        PaymentHistory history = new PaymentHistory(payment, payment.getPaymentStatus(), payment.getAmount(), description, "callback");
        history.setGatewayResponse(gatewayResponse);
        return history;
    }
}