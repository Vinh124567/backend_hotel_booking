package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class BookingValidationService {

    public void validateBookingRequest(BookingRequest request) {
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

    public void validateBookingForConfirmation(Booking booking) {
        String status = booking.getStatus();
        if (!BookingStatus.TEMPORARY.equals(status) && !BookingStatus.PENDING.equals(status)) {
            throw new RuntimeException("Chỉ có thể xác nhận booking ở trạng thái 'Tạm giữ chỗ' hoặc 'Chờ xác nhận'");
        }
    }

    public void validateBookingOwnership(Booking booking, User user) {
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền thao tác với đặt phòng này");
        }
    }

    public void validateCancellation(Booking booking) {
        String status = booking.getStatus();

        if (BookingStatus.CANCELLED.equals(status)) {
            throw new RuntimeException("Đặt phòng đã bị hủy trước đó");
        }

        if (BookingStatus.COMPLETED.equals(status)) {
            throw new RuntimeException("Không thể hủy - Đặt phòng đã hoàn thành");
        }

        if (BookingStatus.CHECKED_IN.equals(status)) {
            throw new RuntimeException("Không thể hủy - Đã nhận phòng");
        }
        if (BookingStatus.TEMPORARY.equals(status)) {
            return;
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        if (daysUntilCheckIn < 1) {
            throw new RuntimeException("Không thể hủy - Quá thời hạn 24 giờ trước check-in");
        }
    }

    public void validateCheckIn(Booking booking) {
        String status = booking.getStatus();

        if (!BookingStatus.TEMPORARY.equals(status) && !BookingStatus.PENDING.equals(status) && !BookingStatus.CONFIRMED.equals(status)) {
            throw new RuntimeException("Chỉ có thể check-in đặt phòng ở trạng thái hợp lệ");
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();

        if (now.isBefore(checkInDate)) {
            throw new RuntimeException("Chưa đến ngày check-in");
        }

        if (ChronoUnit.DAYS.between(checkInDate, now) > 1) {
            throw new RuntimeException("Đã quá thời gian check-in");
        }
    }

    public void validateCheckOut(Booking booking) {
        String status = booking.getStatus();

        if (!BookingStatus.CHECKED_IN.equals(status)) {
            throw new RuntimeException("Chỉ có thể check-out sau khi đã check-in");
        }

        LocalDate checkOutDate = booking.getCheckOutDate();
        LocalDate now = LocalDate.now();

        if (now.isAfter(checkOutDate.plusDays(1))) {
            throw new RuntimeException("Đã quá thời gian check-out");
        }
    }

    public void validateModification(Booking booking) {
        String status = booking.getStatus();

        if (BookingStatus.CANCELLED.equals(status) || BookingStatus.COMPLETED.equals(status) || BookingStatus.CHECKED_IN.equals(status)) {
            throw new RuntimeException("Không thể sửa đổi đặt phòng ở trạng thái hiện tại");
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        if (daysUntilCheckIn < 2) {
            throw new RuntimeException("Không thể sửa đổi - Phải trước 48 giờ so với check-in");
        }
    }
}