package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    /**
     * Tạo booking mới
     */
    BookingResponse createBooking(BookingRequest request);

    /**
     * Lấy thông tin booking theo ID
     */
    BookingResponse getBookingById(Long id);

    /**
     * Lấy danh sách booking của user hiện tại
     */
    List<BookingResponse> getCurrentUserBookings();

    /**
     * Hủy booking
     */
    BookingResponse cancelBooking(Long id);

    /**
     * Kiểm tra room type có available không
     */
    boolean isRoomTypeAvailable(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate);
}