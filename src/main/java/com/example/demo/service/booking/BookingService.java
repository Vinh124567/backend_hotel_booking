// ✅ BookingService.java - UPDATED INTERFACE
package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatsResponse;
import com.example.demo.dto.report.HotelStatsResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BookingService {

    // ✅ EXISTING METHODS (keep as is)
    List<BookingResponse> getCurrentUserPendingBookings();
    BookingResponse createBooking(BookingRequest request);
    BookingResponse getBookingById(Long id);
    List<BookingResponse> getCurrentUserBookings();
    BookingResponse cancelBooking(Long id);
    boolean isRoomTypeAvailable(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate);
    BookingResponse checkInBooking(Long bookingId);
    BookingResponse checkOutBooking(Long bookingId);
    BookingResponse updateBooking(Long bookingId, BookingRequest request);
    BookingStatsResponse getUserBookingStats();
    BookingResponse confirmBooking(Long bookingId);
    List<BookingResponse> getPendingBookings();
    List<BookingResponse> getBookingsReadyForCheckIn();
    List<BookingResponse> getBookingsReadyForCheckOut();
    List<BookingResponse> getCurrentlyCheckedInBookings();
    List<BookingResponse> getCheckHistory();
    List<BookingResponse> getBookingsByHotel(Long hotelId, String status);
    HotelStatsResponse getHotelRevenue(Long hotelId, LocalDate fromDate, LocalDate toDate, String status);

    // ✅ NEW METHODS FOR DEPOSIT PAYMENT
    BookingResponse payDeposit(Long bookingId, BigDecimal depositPercentage);
    BookingResponse payRemaining(Long bookingId);
    BookingResponse payFullAmount(Long bookingId);
}