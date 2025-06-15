package com.example.demo.dto.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
public class BookingRequest {

    @NotNull(message = "RoomType ID không được để trống")
    @Positive(message = "RoomType ID phải là số dương")
    private Long roomTypeId;

    // ✅ THÊM: Optional roomId cho feature chọn phòng cụ thể
    private Long roomId;

    @NotNull(message = "Ngày check-in không được để trống")
    @Future(message = "Ngày check-in phải là tương lai")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    @Future(message = "Ngày check-out phải là tương lai")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;

    @NotNull(message = "Số lượng khách không được để trống")
    @Positive(message = "Số lượng khách phải lớn hơn 0")
    @Max(value = 10, message = "Số lượng khách không được vượt quá 10")
    private Integer numberOfGuests;

    @NotNull(message = "Tổng giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Tổng giá phải lớn hơn 0")
    private Double totalPrice;

    @Size(max = 500, message = "Yêu cầu đặc biệt không được vượt quá 500 ký tự")
    private String specialRequests;

    // Custom validation method
    @AssertTrue(message = "Ngày check-out phải sau ngày check-in")
    public boolean isCheckOutAfterCheckIn() {
        if (checkInDate == null || checkOutDate == null) {
            return true; // Let @NotNull handle null validation
        }
        return checkOutDate.isAfter(checkInDate);
    }

    @AssertTrue(message = "Booking phải ít nhất 1 đêm và không quá 30 đêm")
    public boolean isValidBookingDuration() {
        if (checkInDate == null || checkOutDate == null) {
            return true;
        }
        long days = checkInDate.until(checkOutDate).getDays();
        return days >= 1 && days <= 30;
    }

    // ✅ THÊM: Helper method
    public boolean hasSpecificRoomSelected() {
        return roomId != null;
    }
}