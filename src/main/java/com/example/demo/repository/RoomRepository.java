package com.example.demo.repository;

import com.example.demo.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // ========== BASIC QUERIES ==========

    /**
     * Tìm phòng theo số phòng
     */
    Optional<Room> findByRoomNumber(String roomNumber);

    /**
     * ✅ Tìm phòng theo room type (có sắp xếp)
     */
    List<Room> findByRoomType_IdOrderByRoomNumberAsc(Long roomTypeId);

    /**
     * ✅ Tìm phòng theo số phòng và room type
     */
    Optional<Room> findByRoomNumberAndRoomType_Id(String roomNumber, Long roomTypeId);

    /**
     * ✅ Tìm phòng theo status (có sắp xếp)
     */
    List<Room> findByStatusOrderByRoomNumberAsc(String status);

    /**
     * ✅ Tìm phòng theo floor (có sắp xếp)
     */
    List<Room> findByFloorOrderByRoomNumberAsc(String floor);

    /**
     * ✅ Combined search methods (có sắp xếp)
     */
    List<Room> findByRoomType_IdAndStatusOrderByRoomNumberAsc(Long roomTypeId, String status);

    /**
     * ✅ Tìm phòng theo hotel (có sắp xếp)
     */
    @Query("""
        SELECT r FROM Room r 
        WHERE r.roomType.hotel.id = :hotelId
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findByRoomType_Hotel_IdOrderByRoomNumberAsc(@Param("hotelId") Long hotelId);

    // ========== AVAILABILITY QUERIES ==========

    /**
     * ✅ Tìm phòng trống theo room type và thời gian
     */
    @Query("""
        SELECT r FROM Room r 
        WHERE r.roomType.id = :roomTypeId 
        AND r.status = 'Trống'
        AND r.id NOT IN (
            SELECT DISTINCT b.assignedRoom.id 
            FROM Booking b 
            WHERE b.assignedRoom IS NOT NULL 
            AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã nhận phòng')
            AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)
        )
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findAvailableRoomsByTypeAndDates(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * ✅ Đếm số phòng trống theo room type và thời gian
     */
    @Query("""
        SELECT COUNT(r) FROM Room r 
        WHERE r.roomType.id = :roomTypeId 
        AND r.status = 'Trống'
        AND r.id NOT IN (
            SELECT DISTINCT b.assignedRoom.id 
            FROM Booking b 
            WHERE b.assignedRoom IS NOT NULL 
            AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã nhận phòng')
            AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)
        )
    """)
    long countAvailableRoomsByTypeAndDates(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * ✅ Tìm tất cả phòng available theo hotel và thời gian
     */
    @Query("""
        SELECT r FROM Room r 
        JOIN r.roomType rt 
        WHERE rt.hotel.id = :hotelId
        AND r.status = 'Trống'
        AND r.id NOT IN (
            SELECT DISTINCT b.assignedRoom.id 
            FROM Booking b 
            WHERE b.assignedRoom IS NOT NULL 
            AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã nhận phòng')
            AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)
        )
        ORDER BY rt.typeName ASC, r.roomNumber ASC
    """)
    List<Room> findAvailableRoomsByHotelAndDates(
            @Param("hotelId") Long hotelId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    // ========== CONFLICT CHECKING ==========

    /**
     * ✅ Đếm booking conflict cho room (không exclude booking nào)
     */
    @Query("""
        SELECT COUNT(b)
        FROM Booking b 
        WHERE b.assignedRoom.id = :roomId 
        AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã nhận phòng')
        AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)
    """)
    long countConflictingBookingsForRoom(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * ✅ Đếm booking conflict cho room (exclude 1 booking cụ thể)
     */
    @Query("""
        SELECT COUNT(b)
        FROM Booking b 
        WHERE b.assignedRoom.id = :roomId 
        AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã nhận phòng')
        AND b.id <> :excludeBookingId
        AND (b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)
    """)
    long countConflictingBookingsForRoomExcluding(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeBookingId") Long excludeBookingId
    );

    /**
     * ✅ Check room available - wrapper method
     */
    default boolean isRoomAvailableForDates(Long roomId, LocalDate checkIn, LocalDate checkOut, Long excludeBookingId) {
        if (excludeBookingId == null) {
            return countConflictingBookingsForRoom(roomId, checkIn, checkOut) == 0;
        } else {
            return countConflictingBookingsForRoomExcluding(roomId, checkIn, checkOut, excludeBookingId) == 0;
        }
    }

    // ========== OCCUPANCY QUERIES ==========

    /**
     * Tìm phòng đang được sử dụng
     */
    @Query("""
        SELECT r FROM Room r 
        JOIN Booking b ON b.assignedRoom.id = r.id
        WHERE b.status = 'Đã nhận phòng'
        AND CURRENT_DATE BETWEEN b.checkInDate AND b.checkOutDate
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findCurrentlyOccupiedRooms();

    /**
     * Tìm phòng sẽ check-in hôm nay
     */
    @Query("""
        SELECT r FROM Room r 
        JOIN Booking b ON b.assignedRoom.id = r.id
        WHERE b.checkInDate = :today
        AND b.status IN ('Đã xác nhận', 'Đã thanh toán')
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findRoomsCheckingInToday(@Param("today") LocalDate today);

    /**
     * Tìm phòng sẽ check-out hôm nay
     */
    @Query("""
        SELECT r FROM Room r 
        JOIN Booking b ON b.assignedRoom.id = r.id
        WHERE b.checkOutDate = :today
        AND b.status = 'Đã nhận phòng'
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findRoomsCheckingOutToday(@Param("today") LocalDate today);

    /**
     * Tìm phòng trống (không có booking active)
     */
    @Query("""
        SELECT r FROM Room r 
        WHERE r.status = 'Trống'
        AND r.id NOT IN (
            SELECT DISTINCT b.assignedRoom.id 
            FROM Booking b 
            WHERE b.assignedRoom IS NOT NULL 
            AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận', 'Đã nhận phòng')
            AND CURRENT_DATE BETWEEN b.checkInDate AND b.checkOutDate
        )
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findCurrentlyAvailableRooms();

    // ========== STATISTICS QUERIES ==========

    /**
     * Đếm phòng theo status
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * Đếm phòng theo room type
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.roomType.id = :roomTypeId")
    long countByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    /**
     * Đếm phòng theo hotel
     */
    @Query("""
        SELECT COUNT(r) FROM Room r 
        JOIN r.roomType rt 
        WHERE rt.hotel.id = :hotelId
    """)
    long countByHotelId(@Param("hotelId") Long hotelId);

    /**
     * Tính tỷ lệ lấp đầy theo hotel
     */
    @Query("""
    SELECT (1.0 * COUNT(DISTINCT b.assignedRoom.id)) / COUNT(DISTINCT r.id) * 100
    FROM Room r 
    LEFT JOIN Booking b ON b.assignedRoom.id = r.id 
        AND b.status = 'Đã nhận phòng'
        AND CURRENT_DATE BETWEEN b.checkInDate AND b.checkOutDate
    WHERE r.roomType.hotel.id = :hotelId
""")
    Double getOccupancyRateByHotel(@Param("hotelId") Long hotelId);

    @Query("""
    SELECT (1.0 * COUNT(DISTINCT b.assignedRoom.id)) / COUNT(DISTINCT r.id) * 100
    FROM Room r 
    LEFT JOIN Booking b ON b.assignedRoom.id = r.id 
        AND b.status = 'Đã nhận phòng'
        AND CURRENT_DATE BETWEEN b.checkInDate AND b.checkOutDate
    WHERE r.roomType.id = :roomTypeId
""")
    Double getOccupancyRateByRoomType(@Param("roomTypeId") Long roomTypeId);

    // ========== MAINTENANCE QUERIES ==========

    /**
     * Tìm phòng bảo trì
     */
    @Query("SELECT r FROM Room r WHERE r.status = 'Bảo trì' ORDER BY r.roomNumber ASC")
    List<Room> findMaintenanceRooms();

    /**
     * Tìm phòng có thể chuyển sang bảo trì (không có booking trong tương lai)
     */
    @Query("""
        SELECT r FROM Room r 
        WHERE r.status = 'Trống'
        AND r.id NOT IN (
            SELECT DISTINCT b.assignedRoom.id 
            FROM Booking b 
            WHERE b.assignedRoom IS NOT NULL 
            AND b.status IN ('Tạm giữ chỗ', 'Chờ xác nhận', 'Đã xác nhận')
            AND b.checkInDate >= CURRENT_DATE
        )
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findRoomsAvailableForMaintenance();

    // ========== SEARCH & FILTER QUERIES ==========

    /**
     * Search phòng theo số phòng pattern
     */
    @Query("""
        SELECT r FROM Room r 
        WHERE r.roomNumber LIKE %:pattern%
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findByRoomNumberContaining(@Param("pattern") String pattern);

    /**
     * Tìm phòng theo nhiều tiêu chí
     */
    @Query("""
        SELECT r FROM Room r 
        JOIN r.roomType rt 
        WHERE (:hotelId IS NULL OR rt.hotel.id = :hotelId)
        AND (:roomTypeId IS NULL OR rt.id = :roomTypeId)
        AND (:status IS NULL OR r.status = :status)
        AND (:floor IS NULL OR r.floor = :floor)
        ORDER BY r.roomNumber ASC
    """)
    List<Room> findRoomsByCriteria(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("status") String status,
            @Param("floor") String floor
    );

    /**
     * Tìm phòng với capacity tối thiểu
     */
    @Query("""
        SELECT r FROM Room r 
        JOIN r.roomType rt 
        WHERE rt.maxOccupancy >= :minCapacity
        AND r.status = 'Trống'
        ORDER BY rt.maxOccupancy ASC, r.roomNumber ASC
    """)
    List<Room> findAvailableRoomsWithMinCapacity(@Param("minCapacity") Integer minCapacity);

    /**
     * Tìm phòng trong khoảng giá
     */
    @Query("""
        SELECT r FROM Room r 
        JOIN r.roomType rt 
        WHERE rt.basePrice BETWEEN :minPrice AND :maxPrice
        AND r.status = 'Trống'
        ORDER BY rt.basePrice ASC, r.roomNumber ASC
    """)
    List<Room> findAvailableRoomsInPriceRange(
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );

    // ========== REPORTING QUERIES ==========

    /**
     * Lấy báo cáo tình trạng phòng theo hotel
     */
    @Query("""
        SELECT r.status, COUNT(r) 
        FROM Room r 
        JOIN r.roomType rt 
        WHERE rt.hotel.id = :hotelId
        GROUP BY r.status
    """)
    List<Object[]> getRoomStatusReportByHotel(@Param("hotelId") Long hotelId);

    /**
     * Lấy báo cáo phòng theo tầng
     */
    @Query("""
        SELECT r.floor, COUNT(r) 
        FROM Room r 
        JOIN r.roomType rt 
        WHERE rt.hotel.id = :hotelId
        GROUP BY r.floor
        ORDER BY r.floor ASC
    """)
    List<Object[]> getRoomFloorReportByHotel(@Param("hotelId") Long hotelId);

    /**
     * Lấy top phòng được đặt nhiều nhất
     */
    @Query("""
        SELECT r, COUNT(b) as bookingCount
        FROM Room r 
        LEFT JOIN Booking b ON b.assignedRoom.id = r.id
        WHERE b.status IN ('Đã xác nhận', 'Đã thanh toán', 'Đã nhận phòng', 'Hoàn thành')
        GROUP BY r
        ORDER BY bookingCount DESC
    """)
    List<Object[]> getTopBookedRooms();

    // ========== UTILITY METHODS ==========

    /**
     * Kiểm tra room number có tồn tại trong hotel không
     */
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Room r 
        JOIN r.roomType rt 
        WHERE r.roomNumber = :roomNumber 
        AND rt.hotel.id = :hotelId
    """)
    boolean existsByRoomNumberAndHotelId(
            @Param("roomNumber") String roomNumber,
            @Param("hotelId") Long hotelId
    );

    /**
     * Lấy room numbers theo pattern cho autocomplete
     */
    @Query("""
        SELECT DISTINCT r.roomNumber 
        FROM Room r 
        WHERE r.roomNumber LIKE %:pattern%
        ORDER BY r.roomNumber ASC
    """)
    List<String> findRoomNumbersLike(@Param("pattern") String pattern);
}