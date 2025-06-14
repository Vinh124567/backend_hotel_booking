package com.example.demo.controller;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatsResponse;
import com.example.demo.dto.room.RoomResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.booking.BookingService;
import com.example.demo.service.room.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("api/v1/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class BookingController {

    private final BookingService bookingService;
    private final RoomService roomService;


    /**
     * Tạo booking mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        log.info("Creating booking for roomTypeId: {}, checkIn: {}, checkOut: {}",
                request.getRoomTypeId(), request.getCheckInDate(), request.getCheckOutDate());

        BookingResponse booking = bookingService.createBooking(request);

        ApiResponse<BookingResponse> response = new ApiResponse<>();
        response.setResult(booking);
        response.setCode(HttpStatus.CREATED.value());
        response.setMessage("Đặt phòng thành công");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy thông tin booking theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        BookingResponse booking = bookingService.getBookingById(id);

        ApiResponse<BookingResponse> response = new ApiResponse<>();
        response.setResult(booking);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách booking của user hiện tại
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCurrentUserBookings() {
        List<BookingResponse> bookings = bookingService.getCurrentUserBookings();

        ApiResponse<List<BookingResponse>> response = new ApiResponse<>();
        response.setResult(bookings);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách booking thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * Hủy booking
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {
        log.info("Cancelling booking: {}", id);

        BookingResponse booking = bookingService.cancelBooking(id);

        ApiResponse<BookingResponse> response = new ApiResponse<>();
        response.setResult(booking);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Hủy đặt phòng thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Check-in booking
     */
    @PutMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<BookingResponse>> checkInBooking(@PathVariable Long id) {
        log.info("Check-in booking: {}", id);

        BookingResponse booking = bookingService.checkInBooking(id);

        ApiResponse<BookingResponse> response = new ApiResponse<>();
        response.setResult(booking);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Check-in thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Check-out booking
     */
    @PutMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<BookingResponse>> checkOutBooking(@PathVariable Long id) {
        log.info("Check-out booking: {}", id);

        BookingResponse booking = bookingService.checkOutBooking(id);

        ApiResponse<BookingResponse> response = new ApiResponse<>();
        response.setResult(booking);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Check-out thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Cập nhật booking (modify)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingRequest request) {
        log.info("Updating booking: {}", id);

        BookingResponse booking = bookingService.updateBooking(id, request);

        ApiResponse<BookingResponse> response = new ApiResponse<>();
        response.setResult(booking);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Cập nhật đặt phòng thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra room type có available không
     */
    @GetMapping("/check-availability")
    public ResponseEntity<ApiResponse<Boolean>> checkRoomTypeAvailability(
            @RequestParam Long roomTypeId,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate) {

        // Parse dates (format: yyyy-MM-dd)
        java.time.LocalDate checkIn = java.time.LocalDate.parse(checkInDate);
        java.time.LocalDate checkOut = java.time.LocalDate.parse(checkOutDate);

        boolean isAvailable = bookingService.isRoomTypeAvailable(roomTypeId, checkIn, checkOut);

        ApiResponse<Boolean> response = new ApiResponse<>();
        response.setResult(isAvailable);
        response.setCode(HttpStatus.OK.value());
        response.setMessage(isAvailable ? "Phòng khả dụng" : "Phòng đã hết");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Lấy thống kê booking của user
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<BookingStatsResponse>> getBookingStats() {
        log.info("Getting booking stats for current user");

        BookingStatsResponse stats = bookingService.getUserBookingStats();

        ApiResponse<BookingStatsResponse> response = new ApiResponse<>();
        response.setResult(stats);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy thống kê thành công");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable Long id) {
        log.info("Confirming booking: {}", id);

        BookingResponse booking = bookingService.confirmBooking(id);

        ApiResponse<BookingResponse> response = new ApiResponse<>();
        response.setResult(booking);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Xác nhận đặt phòng thành công");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/pending-confirmation")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getPendingBookings() {
        log.info("Getting all pending confirmation bookings");

        List<BookingResponse> bookings = bookingService.getPendingBookings();

        ApiResponse<List<BookingResponse>> response = new ApiResponse<>();
        response.setResult(bookings);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách booking chờ xác nhận thành công");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getMyPendingBookings() {
        List<BookingResponse> pendingBookings = bookingService.getCurrentUserPendingBookings();
        ApiResponse<List<BookingResponse>> response = new ApiResponse<>();
        response.setResult(pendingBookings);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách booking tạm giữ chỗ thành công");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    /**
     * ✅ Lấy danh sách booking sẵn sàng check-in hôm nay
     */
    @GetMapping("/ready-for-checkin")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsReadyForCheckIn() {
        log.info("Getting bookings ready for check-in");

        List<BookingResponse> bookings = bookingService.getBookingsReadyForCheckIn();

        ApiResponse<List<BookingResponse>> response = new ApiResponse<>();
        response.setResult(bookings);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách booking sẵn sàng check-in thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Lấy danh sách booking sẵn sàng check-out hôm nay
     */
    @GetMapping("/ready-for-checkout")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_STAFF')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsReadyForCheckOut() {
        log.info("Getting bookings ready for check-out");

        List<BookingResponse> bookings = bookingService.getBookingsReadyForCheckOut();

        ApiResponse<List<BookingResponse>> response = new ApiResponse<>();
        response.setResult(bookings);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách booking sẵn sàng check-out thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Lấy danh sách booking đang ở khách sạn (đã check-in)
     */
    @GetMapping("/checked-in")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_STAFF')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCurrentlyCheckedInBookings() {
        log.info("Getting currently checked-in bookings");

        List<BookingResponse> bookings = bookingService.getCurrentlyCheckedInBookings();

        ApiResponse<List<BookingResponse>> response = new ApiResponse<>();
        response.setResult(bookings);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách booking đã check-in thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Lấy lịch sử check-in/out của user hiện tại
     */
    @GetMapping("/check-history")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCheckHistory() {
        log.info("Getting user's check-in/out history");

        List<BookingResponse> bookings = bookingService.getCheckHistory();

        ApiResponse<List<BookingResponse>> response = new ApiResponse<>();
        response.setResult(bookings);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy lịch sử check-in/out thành công");

        return ResponseEntity.ok(response);
    }

}
