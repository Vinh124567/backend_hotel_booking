package com.example.demo.service.booking;

import com.example.demo.entity.Booking;
import com.example.demo.entity.Room;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class BookingAvailabilityService {
    private static final Logger log = LoggerFactory.getLogger(BookingAvailabilityService.class);

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    // ✅ SỬA: Throw exception nếu roomType không tồn tại
    public boolean isRoomTypeAvailable(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại"));

        long availableRooms = roomRepository.countAvailableRoomsByTypeAndDates(
                roomTypeId, checkInDate, checkOutDate);

        return availableRooms > 0;
    }

    // ✅ Giữ nguyên
    public boolean isRoomTypeAvailableForConfirmation(Booking booking) {
        long availableRooms = roomRepository.countAvailableRoomsByTypeAndDates(
                booking.getRoomType().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate());

        return availableRooms > 0;
    }

    // ✅ Giữ nguyên
    public Room findAvailableRoom(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        List<Room> availableRooms = roomRepository.findAvailableRoomsByTypeAndDates(
                roomTypeId, checkInDate, checkOutDate);

        return availableRooms.isEmpty() ? null : availableRooms.get(0);
    }

    // ✅ SỬA: Throw lỗi nếu repository lỗi
    public boolean isSpecificRoomAvailable(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        // Kiểm tra nếu room không tồn tại
        roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Phòng không tồn tại"));

        boolean available = roomRepository
                .isRoomAvailableForDates(roomId, checkInDate, checkOutDate, null);

        if (!available) {
            throw new RuntimeException("Phòng đã được đặt trong khoảng thời gian này, vui lòng chọn khoảng thời gian khác");
        }

        return true;
    }

}
