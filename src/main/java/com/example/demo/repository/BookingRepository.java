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

    // ========== BASIC QUERIES ==========

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdOrderByBookingDateDesc(Long userId);

    List<Booking> findByStatusOrderByBookingDateDesc(String status);

    List<Booking> findByUserIdAndStatus(Long userId, String status);

    List<Booking> findByAssignedRoomId(Long roomId);

    // ========== AVAILABILITY CHECK QUERIES ==========

    /**
     * Đếm booking confirmed overlapping theo roomType (OLD METHOD - DEPRECATED)
     */
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

    /**
     * Đếm booking confirmed overlapping theo roomType (exclude booking cụ thể)
     */
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

    /**
     * Đếm booking pending của user trong khoảng thời gian
     */
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

    // ========== ROOM ASSIGNMENT QUERIES ==========

    /**
     * ✅ Tìm booking đã confirmed nhưng chưa có assigned room
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status IN ('Đã xác nhận', 'Đã thanh toán') 
        AND b.assignedRoom IS NULL
        ORDER BY b.bookingDate ASC
    """)
    List<Booking> findConfirmedBookingsWithoutAssignedRoom();

    /**
     * ✅ Kiểm tra room có bị conflict với booking khác không
     */
    @Query("""
        SELECT COUNT(b)
        FROM Booking b 
        WHERE b.assignedRoom.id = :roomId 
        AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã nhận phòng')
        AND b.id <> :excludeBookingId
        AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)
    """)
    long countRoomConflicts(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeBookingId") Long excludeBookingId
    );

    /**
     * ✅ Helper method để check room conflict
     */
    default boolean isRoomConflicted(Long roomId, LocalDate checkIn, LocalDate checkOut, Long excludeBookingId) {
        return countRoomConflicts(roomId, checkIn, checkOut, excludeBookingId) > 0;
    }

    // ========== TEMPORARY BOOKING MANAGEMENT ==========

    /**
     * Lấy booking tạm thời chưa hết hạn của user
     */
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

    /**
     * Tìm booking tạm thời đã hết hạn
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'Tạm giữ chỗ' 
        AND b.bookingDate < :cutoffTime
    """)
    List<Booking> findExpiredTemporaryBookings(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Tìm booking pending cũ không có payment thành công
     */
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

    // ========== DETAILED FETCH QUERIES ==========

    /**
     * Lấy booking với tất cả thông tin liên quan theo ID
     */
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

    /**
     * Lấy booking của user với tất cả thông tin liên quan
     */
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

    /**
     * Lấy booking theo status với tất cả thông tin liên quan
     */
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

    // ========== STATISTICS QUERIES ==========

    /**
     * Đếm booking theo status
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * Kiểm tra user có booking active không
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END 
        FROM Booking b 
        WHERE b.user.id = :userId 
        AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã thanh toán')
    """)
    boolean hasActiveBookings(@Param("userId") Long userId);

    /**
     * Kiểm tra booking có payment thành công không
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END 
        FROM Payment p 
        WHERE p.booking.id = :bookingId 
        AND p.paymentStatus = 'Đã thanh toán'
    """)
    boolean existsSuccessfulPaymentForBooking(@Param("bookingId") Long bookingId);

    // ========== HOTEL MANAGEMENT QUERIES ==========

    /**
     * Lấy booking theo hotel ID
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.roomType.hotel.id = :hotelId
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByHotelId(@Param("hotelId") Long hotelId);

    /**
     * Lấy booking check-in hôm nay
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.checkInDate = :today
        AND b.status IN ('Đã xác nhận', 'Đã thanh toán')
        ORDER BY b.bookingDate ASC
    """)
    List<Booking> findTodayCheckIns(@Param("today") LocalDate today);

    /**
     * Lấy booking check-out hôm nay
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.checkOutDate = :today
        AND b.status = 'Đã nhận phòng'
        ORDER BY b.bookingDate ASC
    """)
    List<Booking> findTodayCheckOuts(@Param("today") LocalDate today);

    /**
     * Lấy booking hiện đang ở khách sạn (checked-in)
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'Đã nhận phòng'
        AND CURRENT_DATE >= b.checkInDate 
        AND CURRENT_DATE < b.checkOutDate
        ORDER BY b.assignedRoom.roomNumber ASC
    """)
    List<Booking> findCurrentlyCheckedInBookings();

    // ========== DATE RANGE QUERIES ==========

    /**
     * Lấy booking trong khoảng thời gian
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.checkInDate >= :startDate 
        AND b.checkOutDate <= :endDate
        ORDER BY b.checkInDate ASC
    """)
    List<Booking> findBookingsInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Lấy booking overlap với khoảng thời gian
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.checkInDate < :endDate 
        AND b.checkOutDate > :startDate
        AND b.status IN ('Đã xác nhận', 'Đã thanh toán', 'Đã nhận phòng')
        ORDER BY b.checkInDate ASC
    """)
    List<Booking> findBookingsOverlappingDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ========== REVENUE QUERIES ==========

    /**
     * Tính tổng doanh thu theo tháng
     */
    @Query("""
        SELECT SUM(b.totalPrice) FROM Booking b 
        WHERE b.status IN ('Đã xác nhận', 'Đã thanh toán', 'Đã nhận phòng', 'Hoàn thành')
        AND YEAR(b.bookingDate) = :year 
        AND MONTH(b.bookingDate) = :month
    """)
    Double getTotalRevenueByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Tính tổng doanh thu của user
     */
    @Query("""
        SELECT SUM(b.totalPrice) FROM Booking b 
        WHERE b.user.id = :userId
        AND b.status IN ('Đã xác nhận', 'Đã thanh toán', 'Đã nhận phòng', 'Hoàn thành')
    """)
    Double getTotalSpentByUser(@Param("userId") Long userId);

    @Query("""
    SELECT COUNT(b) FROM Booking b 
    WHERE b.user.id = :userId 
    AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã nhận phòng')
""")
    long countUserActiveBookings(@Param("userId") Long userId);

    // ========== ✅ REVIEW VALIDATION QUERIES (THÊM MỚI) ==========

    /**
     * Kiểm tra user có booking completed cho hotel này không
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END 
        FROM Booking b 
        WHERE b.user.id = :userId 
        AND b.roomType.hotel.id = :hotelId 
        AND b.status = 'Hoàn thành'
    """)
    boolean hasCompletedBookingForHotel(@Param("userId") Long userId, @Param("hotelId") Long hotelId);

    /**
     * Lấy danh sách hotels mà user đã hoàn thành booking nhưng chưa review
     */
    @Query("""
        SELECT DISTINCT b.roomType.hotel.id 
        FROM Booking b 
        WHERE b.user.id = :userId 
        AND b.status = 'Hoàn thành'
        AND b.roomType.hotel.id NOT IN (
            SELECT r.hotel.id FROM Review r WHERE r.user.id = :userId
        )
    """)
    List<Long> findHotelsEligibleForReview(@Param("userId") Long userId);
}