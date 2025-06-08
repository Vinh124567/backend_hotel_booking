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

    // ========== EXISTING METHODS ========== (keep all existing methods)
    List<Booking> findByStatusAndBookingDateBefore(String status, LocalDateTime cutoffTime);
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingDate < :cutoffTime")
    List<Booking> findExpiredPendingBookings(@Param("status") String status, @Param("cutoffTime") LocalDateTime cutoffTime);
    /**
     * Tìm tất cả booking của user theo ID
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Lấy danh sách booking của user theo ID, sắp xếp theo ngày booking mới nhất
     */
    List<Booking> findByUserIdOrderByBookingDateDesc(Long userId);

    /**
     * Kiểm tra room type có available không trong khoảng thời gian
     * Đếm số booking overlap với khoảng thời gian cho room type cụ thể
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b 
        WHERE b.roomType.id = :roomTypeId 
        AND b.status NOT IN ('Đã hủy', 'NO_SHOW') 
        AND (
            (b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate)
        )
    """)
    long countOverlappingBookings(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    /**
     * Tìm booking theo trạng thái
     */
    List<Booking> findByStatusOrderByBookingDateDesc(String status);

    /**
     * Đếm số phòng available của room type trong database
     * (Giả sử RoomType có field totalRooms hoặc count từ Room entity)
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.roomType.id = :roomTypeId AND r.status = 'AVAILABLE'")
    long countAvailableRoomsByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    /**
     * Tìm booking theo user và status
     */
    List<Booking> findByUserIdAndStatus(Long userId, String status);

    /**
     * Kiểm tra user có booking active không
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END 
        FROM Booking b 
        WHERE b.user.id = :userId 
        AND b.status IN ('Chờ xác nhận', 'Đã xác nhận', 'Đã thanh toán')
    """)
    boolean hasActiveBookings(@Param("userId") Long userId);

    /**
     * Tìm booking theo assigned room ID
     */
    List<Booking> findByAssignedRoomId(Long roomId);

    /**
     * Tìm booking cần thanh toán (chờ xác nhận)
     */
    List<Booking> findByStatusAndUserIdOrderByBookingDateDesc(String status, Long userId);

    // ========== ✅ NEW OPTIMIZED METHODS WITH EAGER LOADING ==========

    /**
     * Tìm booking theo ID với tất cả thông tin liên quan (EAGER LOADING)
     * Tránh N+1 query problem bằng cách join fetch tất cả related entities
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
        WHERE b.id = :bookingId
    """)
    Optional<Booking> findByIdWithDetails(@Param("bookingId") Long bookingId);

    /**
     * Lấy danh sách booking của user với tất cả thông tin liên quan (EAGER LOADING)
     * Sắp xếp theo ngày booking mới nhất
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
        WHERE b.user.id = :userId
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByUserIdWithDetailsOrderByBookingDateDesc(@Param("userId") Long userId);

    /**
     * Lấy danh sách booking theo status với thông tin liên quan (EAGER LOADING)
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH h.images hi
        LEFT JOIN FETCH b.assignedRoom ar
        WHERE b.status = :status
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByStatusWithDetailsOrderByBookingDateDesc(@Param("status") String status);

    /**
     * Tìm booking theo user và status với thông tin liên quan (EAGER LOADING)
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH h.images hi
        LEFT JOIN FETCH b.assignedRoom ar
        WHERE b.user.id = :userId AND b.status = :status
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByUserIdAndStatusWithDetails(@Param("userId") Long userId, @Param("status") String status);

    /**
     * Lấy tất cả booking với thông tin đầy đủ (cho Admin)
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH h.images hi
        LEFT JOIN FETCH b.assignedRoom ar
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findAllWithDetailsOrderByBookingDateDesc();

    /**
     * Lấy booking theo hotel ID với thông tin đầy đủ
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH h.images hi
        LEFT JOIN FETCH b.assignedRoom ar
        WHERE h.id = :hotelId
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByHotelIdWithDetails(@Param("hotelId") Long hotelId);

    /**
     * Tìm booking trong khoảng thời gian với thông tin đầy đủ
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH h.images hi
        LEFT JOIN FETCH b.assignedRoom ar
        WHERE b.checkInDate >= :startDate AND b.checkOutDate <= :endDate
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findByDateRangeWithDetails(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Thống kê booking theo user với thông tin cơ bản (không cần eager load)
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.user.id = :userId
    """)
    List<Booking> findByUserIdForStats(@Param("userId") Long userId);

    /**
     * Tìm booking sắp check-in (trong vòng 24h) với thông tin đầy đủ
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.roomType rt
        LEFT JOIN FETCH rt.hotel h
        LEFT JOIN FETCH h.location loc
        LEFT JOIN FETCH b.assignedRoom ar
        WHERE b.checkInDate = :tomorrow
        AND b.status IN ('Chờ xác nhận', 'Đã xác nhận')
        ORDER BY b.bookingDate DESC
    """)
    List<Booking> findUpcomingCheckInsWithDetails(@Param("tomorrow") LocalDate tomorrow);

    /**
     * Tìm booking theo room type với thông tin cơ bản (cho availability check)
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.roomType.id = :roomTypeId
        AND b.status NOT IN ('Đã hủy', 'NO_SHOW')
        AND (
            (b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate)
        )
    """)
    List<Booking> findOverlappingBookings(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );
}