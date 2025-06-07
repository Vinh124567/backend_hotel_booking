package com.example.demo.repository;
import com.example.demo.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    // Lấy phòng theo room type ID
    List<Room> findByRoomType_Id(Long roomTypeId);

    // Lấy phòng theo room type ID và status
    List<Room> findByRoomType_IdAndStatus(Long roomTypeId, String status);

    // Lấy phòng trống theo room type ID
    @Query("SELECT r FROM Room r WHERE r.roomType.id = :roomTypeId AND r.status = 'Trống'")
    List<Room> findAvailableRoomsByRoomTypeId(@Param("roomTypeId") Long roomTypeId);

    // Lấy phòng theo room type ID, sắp xếp theo số phòng
    @Query("SELECT r FROM Room r WHERE r.roomType.id = :roomTypeId ORDER BY r.roomNumber ASC")
    List<Room> findByRoomTypeIdOrderByRoomNumber(@Param("roomTypeId") Long roomTypeId);
}

