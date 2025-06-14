package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatsResponse;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    List<BookingResponse> getCurrentUserPendingBookings();
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


    public BookingResponse checkInBooking(Long bookingId);

    public BookingResponse checkOutBooking(Long bookingId);


    public BookingResponse updateBooking(Long bookingId, BookingRequest request);

    public BookingStatsResponse getUserBookingStats();
    BookingResponse confirmBooking(Long bookingId);
    List<BookingResponse> getPendingBookings();

    /**
     * Lấy danh sách booking sẵn sàng check-in hôm nay
     */
    List<BookingResponse> getBookingsReadyForCheckIn();

    /**
     * Lấy danh sách booking sẵn sàng check-out hôm nay
     */
    List<BookingResponse> getBookingsReadyForCheckOut();

    /**
     * Lấy danh sách booking đang ở khách sạn (đã check-in)
     */
    List<BookingResponse> getCurrentlyCheckedInBookings();

    /**
     * Lấy lịch sử check-in/out của user hiện tại
     */
    List<BookingResponse> getCheckHistory();

//    /**
//     * Admin: Check-in hàng loạt
//     */
//    List<BookingResponse> batchCheckIn(List<Long> bookingIds);
//
//    /**
//     * Admin: Check-out hàng loạt
//     */
//    List<BookingResponse> batchCheckOut(List<Long> bookingIds);
//
//    /**
//     * Admin: Force check-in (bỏ qua validation)
//     */
//    BookingResponse forceCheckIn(Long bookingId);
//
//    /**
//     * Admin: Force check-out (bỏ qua validation)
//     */
//    BookingResponse forceCheckOut(Long bookingId);

}