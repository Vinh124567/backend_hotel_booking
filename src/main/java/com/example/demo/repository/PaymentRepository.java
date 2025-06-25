// PaymentRepository.java - Thêm methods cho MoMo
package com.example.demo.repository;

import com.example.demo.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Basic queries
    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    // MoMo specific queries
    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByRequestId(String requestId);

    // Advanced queries
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId AND p.paymentStatus = :status")
    List<Payment> findByBookingIdAndStatus(@Param("bookingId") Long bookingId, @Param("status") String status);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = 'Chờ thanh toán' AND p.qrExpiryTime < CURRENT_TIMESTAMP")
    List<Payment> findExpiredPayments();

    @Query("SELECT p FROM Payment p WHERE p.gateway = 'momo' AND p.paymentStatus = 'Chờ thanh toán'")
    List<Payment> findPendingMoMoPayments();

    // Count methods
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.booking.id = :bookingId AND p.paymentStatus = 'Đã thanh toán'")
    Long countPaidPaymentsByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = 'Đã thanh toán' AND DATE(p.paymentDate) = CURRENT_DATE")
    Long countTodayPaidPayments();

    // ========== DASHBOARD QUERIES (THÊM VÀO PaymentRepository) ==========

    /**
     * Đếm payment theo status (cho dashboard)
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = :paymentStatus")
    Long countByPaymentStatus(@Param("paymentStatus") String paymentStatus);

    /**
     * Tính tổng doanh thu hôm nay (cho dashboard)
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = 'Đã thanh toán' AND DATE(p.paymentDate) = :date")
    Optional<BigDecimal> getTodayRevenue(@Param("date") LocalDate date);
}