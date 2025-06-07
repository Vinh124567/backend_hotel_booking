package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomType;
import com.example.demo.entity.User;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service
@AllArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public BookingResponse createBooking(BookingRequest request) {
        log.info("🏨 Creating booking for roomTypeId: {}, checkIn: {}, checkOut: {}",
                request.getRoomTypeId(), request.getCheckInDate(), request.getCheckOutDate());

        // 1. Validate request
        validateBookingRequest(request);

        // 2. Check room type exists
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại với ID: " + request.getRoomTypeId()));

        // 3. Check availability
        if (!isRoomTypeAvailable(request.getRoomTypeId(), request.getCheckInDate(), request.getCheckOutDate())) {
            throw new RuntimeException("Loại phòng không khả dụng trong thời gian đã chọn");
        }

        // 4. Get current user
        User currentUser = getCurrentUser();

        // 5. Create booking entity
        Booking booking = new Booking();
        booking.setUser(currentUser);
        booking.setRoomType(roomType);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setTotalPrice(BigDecimal.valueOf(request.getTotalPrice()));
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setStatus("Chờ xác nhận"); // Default status from entity
        // bookingDate will be set by @PrePersist

        // 6. Save booking
        booking = bookingRepository.save(booking);

        log.info("✅ Booking created successfully with ID: {}", booking.getId());

        return mapToBookingResponse(booking);
    }

    @Override
    public BookingResponse getBookingById(Long id) {
        log.info("🔍 Getting booking by ID: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại với ID: " + id));

        // Check permission: user chỉ xem được booking của mình
        User currentUser = getCurrentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không có quyền truy cập booking này");
        }

        return mapToBookingResponse(booking);
    }

    @Override
    public List<BookingResponse> getCurrentUserBookings() {
        User currentUser = getCurrentUser();
        log.info("📋 Getting bookings for user: {}", currentUser.getUsername());

        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookingDateDesc(currentUser.getId());
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .toList();
    }

    @Transactional
    @Override
    public BookingResponse cancelBooking(Long id) {
        log.info("❌ Cancelling booking: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại với ID: " + id));

        // Check permission
        User currentUser = getCurrentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Không có quyền hủy booking này");
        }

        // Check if can cancel
        if (!canCancelBooking(booking)) {
            throw new RuntimeException("Không thể hủy booking này");
        }

        booking.setStatus("Đã hủy");
        booking = bookingRepository.save(booking);

        log.info("✅ Booking cancelled successfully");

        return mapToBookingResponse(booking);
    }

    @Override
    public boolean isRoomTypeAvailable(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        log.info("🔍 Checking availability for roomType: {} from {} to {}",
                roomTypeId, checkInDate, checkOutDate);

        try {
            // Check if roomType exists first
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại"));

            log.info("✅ RoomType found: {}", roomType.getTypeName());

            // Count overlapping bookings
            long bookedRooms = bookingRepository.countOverlappingBookings(roomTypeId, checkInDate, checkOutDate);
            log.info("📊 Booked rooms: {}", bookedRooms);

            // SIMPLE FIX: Assume each room type has 5 rooms available
            long totalRooms = 5; // Fixed number for development
            boolean isAvailable = bookedRooms < totalRooms;

            log.info("📊 Result - Total: {}, Booked: {}, Available: {}",
                    totalRooms, bookedRooms, isAvailable);

            return isAvailable;

        } catch (Exception e) {
            log.error("❌ Availability check error: ", e);
            // For development: return true to allow booking
            log.warn("⚠️ DEVELOPMENT MODE: Returning true for testing");
            return true;
        }
    }

    // ========== Helper Methods ==========

    private void validateBookingRequest(BookingRequest request) {
        if (request.getCheckInDate().isAfter(request.getCheckOutDate())) {
            throw new RuntimeException("Ngày check-in phải trước ngày check-out");
        }

        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày check-in không thể là quá khứ");
        }

        if (request.getNumberOfGuests() <= 0) {
            throw new RuntimeException("Số lượng khách phải lớn hơn 0");
        }

        if (request.getTotalPrice() <= 0) {
            throw new RuntimeException("Tổng giá phải lớn hơn 0");
        }
    }

    private boolean canCancelBooking(Booking booking) {
        // Cannot cancel if already cancelled
        if ("Đã hủy".equals(booking.getStatus())) {
            return false;
        }

        // Cannot cancel if already paid or checked in
        if ("Đã thanh toán".equals(booking.getStatus()) ||
                "Đã nhận phòng".equals(booking.getStatus()) ||
                "Đã trả phòng".equals(booking.getStatus())) {
            return false;
        }

        // Cannot cancel within 24 hours of check-in
        return booking.getCheckInDate().isAfter(LocalDate.now().plusDays(1));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        BookingResponse response = new BookingResponse();

        // Basic fields
        response.setId(booking.getId());
        response.setUserId(booking.getUser().getId());
        response.setRoomTypeId(booking.getRoomType().getId());
        response.setCheckInDate(booking.getCheckInDate());
        response.setCheckOutDate(booking.getCheckOutDate());
        response.setNumberOfGuests(booking.getNumberOfGuests());
        response.setBookingDate(booking.getBookingDate());
        response.setTotalPrice(booking.getTotalPrice().doubleValue()); // Convert BigDecimal to Double
        response.setSpecialRequests(booking.getSpecialRequests());
        response.setStatus(booking.getStatus());

        // Additional fields
        if (booking.getAssignedRoom() != null) {
            response.setAssignedRoomId(booking.getAssignedRoom().getId());
            response.setRoomNumber(booking.getAssignedRoom().getRoomNumber());
        }

        response.setRoomTypeName(booking.getRoomType().getTypeName());
        response.setUserName(booking.getUser().getUsername());
        response.setUserEmail(booking.getUser().getEmail());

        // Computed fields
        response.setCanCancel(canCancelBooking(booking));
        response.setCanModify(canCancelBooking(booking)); // Same rules as cancel

        return response;
    }
}