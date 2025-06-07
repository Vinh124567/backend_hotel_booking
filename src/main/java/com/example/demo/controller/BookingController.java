package com.example.demo.controller;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.booking.BookingService;
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
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Booking service is running");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }
}