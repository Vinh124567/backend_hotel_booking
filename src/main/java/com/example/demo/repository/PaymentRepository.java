// ✅ PaymentRepository.java - VERSION ĐƠN GIẢN CHO ĐỒ ÁN
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

    // ========== EXISTING METHODS (GIỮ NGUYÊN) ==========
    List<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByRequestId(String requestId);

    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId AND p.paymentStatus = :status")
    List<Payment> findByBookingIdAndStatus(@Param("bookingId") Long bookingId, @Param("status") String status);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = 'Chờ thanh toán' AND p.qrExpiryTime < CURRENT_TIMESTAMP")
    List<Payment> findExpiredPayments();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.booking.id = :bookingId AND p.paymentStatus = 'Đã thanh toán'")
    Long countPaidPaymentsByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = :paymentStatus")
    Long countByPaymentStatus(@Param("paymentStatus") String paymentStatus);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = 'Đã thanh toán' AND DATE(p.paymentDate) = :date")
    Optional<BigDecimal> getTodayRevenue(@Param("date") LocalDate date);

    // ========== 🆕 CHỈ 5 METHODS MỚI CẦN THIẾT ==========

    /**
     * 1. Tìm payment theo booking và type
     */
    List<Payment> findByBookingIdAndPaymentType(Long bookingId, Payment.PaymentType paymentType);

    /**
     * 2. Kiểm tra booking đã có payment thành công chưa
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.booking.id = :bookingId AND p.paymentStatus = 'Đã thanh toán'")
    boolean existsPaidPaymentByBookingId(@Param("bookingId") Long bookingId);

    /**
     * 3. Tìm payment cuối cùng của booking
     */
    Payment findTopByBookingIdOrderByCreatedAtDesc(Long bookingId);

    /**
     * 4. Tính tổng tiền đã thanh toán của booking
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.booking.id = :bookingId AND p.paymentStatus = 'Đã thanh toán'")
    BigDecimal getTotalPaidAmountByBookingId(@Param("bookingId") Long bookingId);

    /**
     * 5. Tìm tất cả payments của booking theo thời gian
     */
    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}