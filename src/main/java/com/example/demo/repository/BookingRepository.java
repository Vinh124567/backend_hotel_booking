package com.example.demo.repository;

import com.example.demo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdOrderByBookingDateDesc(Long userId);

    List<Booking> findByStatusOrderByBookingDateDesc(String status);

    List<Booking> findByUserIdAndStatus(Long userId, String status);

    List<Booking> findByAssignedRoomId(Long roomId);

    @Query("""
        SELECT COUNT(b) FROM Booking b 
        WHERE b.roomType.id = :roomTypeId 
        AND b.status IN ('Đã xác nhận', 'Đã thanh toán', 'Đã nhận phòng', 'Checked In') 
        AND NOT (b.checkOutDate <= :checkInDate OR b.checkInDate >= :checkOutDate)
    """)
    long countConfirmedOverlappingBookings(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    @Query("""
    SELECT DISTINCT b FROM Booking b
    LEFT JOIN FETCH b.user u
    LEFT JOIN FETCH b.roomType rt
    LEFT JOIN FETCH rt.hotel h
    LEFT JOIN FETCH h.location loc
    LEFT JOIN FETCH b.payments p
    WHERE b.user.id = :userId 
    AND b.status = 'Tạm giữ chỗ'
    AND b.bookingDate > :cutoffTime
    ORDER BY b.bookingDate DESC
""")
    List<Booking> findUserTemporaryBookingsNotExpired(
            @Param("userId") Long userId,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("""
        SELECT COUNT(b) FROM Booking b 
        WHERE b.roomType.id = :roomTypeId 
        AND b.status IN ('Đã xác nhận', 'Đã thanh toán', 'Đã nhận phòng', 'Checked In') 
        AND b.id <> :excludeBookingId 
        AND NOT (b.checkOutDate <= :checkInDate OR b.checkInDate >= :checkOutDate)
    """)
    long countConfirmedOverlappingBookingsExcluding(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludeBookingId") Long excludeBookingId
    );

    @Query("""
        SELECT COUNT(b) FROM Booking b 
        WHERE b.user.id = :userId 
        AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận') 
        AND NOT (b.checkOutDate <= :checkInDate OR b.checkInDate >= :checkOutDate)
    """)
    long countUserPendingBookingsForDates(
            @Param("userId") Long userId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'Tạm giữ chỗ' 
        AND b.bookingDate < :cutoffTime
    """)
    List<Booking> findExpiredTemporaryBookings(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'Chờ xác nhận' 
        AND b.bookingDate < :cutoffTime 
        AND NOT EXISTS (
            SELECT p FROM Payment p 
            WHERE p.booking = b 
            AND p.paymentStatus = 'Đã thanh toán'
        )
    """)
    List<Booking> findOldPendingBookingsWithoutPayment(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH h.images hi
        LEFT JOIN FETCH h.reviews hr
        LEFT JOIN FETCH rt.amenities rta
        LEFT JOIN FETCH b.assignedRoom ar
        LEFT JOIN FETCH b.payments p
        WHERE b.id = :bookingId
    """)
    Optional<Booking> findByIdWithDetails(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH h.images hi
        LEFT JOIN FETCH h.reviews hr
        LEFT JOIN FETCH rt.amenities rta
        LEFT JOIN FETCH b.assignedRoom ar
        LEFT JOIN FETCH b.payments p
        WHERE b.user.id = :userId
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByUserIdWithDetailsOrderByBookingDateDesc(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH h.images hi
        LEFT JOIN FETCH b.assignedRoom ar
        LEFT JOIN FETCH b.payments p
        WHERE b.status = :status
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByStatusWithDetailsOrderByBookingDateDesc(@Param("status") String status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END 
        FROM Booking b 
        WHERE b.user.id = :userId 
        AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã thanh toán')
    """)
    boolean hasActiveBookings(@Param("userId") Long userId);

    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END 
        FROM Payment p 
        WHERE p.booking.id = :bookingId 
        AND p.paymentStatus = 'Đã thanh toán'
    """)
    boolean existsSuccessfulPaymentForBooking(@Param("bookingId") Long bookingId);
}