package com.example.demo.repository;

import com.example.demo.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

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


}