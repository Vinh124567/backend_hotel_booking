package com.example.demo.service.booking;
import com.example.demo.entity.Booking;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BookingAvailabilityService {
    private static final Logger log = LoggerFactory.getLogger(BookingAvailabilityService.class);
    private static final long TOTAL_ROOMS_PER_TYPE = 5;

    private final BookingRepository bookingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    public boolean isRoomTypeAvailable(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        try {
            roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại"));

            long confirmedBookings = bookingRepository.countConfirmedOverlappingBookings(
                    roomTypeId, checkInDate, checkOutDate);

            return confirmedBookings < TOTAL_ROOMS_PER_TYPE;

        } catch (Exception e) {
            log.error("Availability check error: ", e);
            return false;
        }
    }

    public boolean hasUserPendingBookingForDates(Long userId, LocalDate checkIn, LocalDate checkOut) {
        long pendingCount = bookingRepository.countUserPendingBookingsForDates(userId, checkIn, checkOut);
        return pendingCount > 0;
    }

    public boolean isRoomTypeAvailableForConfirmation(Booking booking) {
        long confirmedBookings = bookingRepository.countConfirmedOverlappingBookingsExcluding(
                booking.getRoomType().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getId());

        return confirmedBookings < TOTAL_ROOMS_PER_TYPE;
    }
}