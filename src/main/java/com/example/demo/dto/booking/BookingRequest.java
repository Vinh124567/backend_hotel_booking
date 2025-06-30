// ✅ BookingRequest.java - FIXED
package com.example.demo.dto.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Data
public class BookingRequest {

    @NotNull(message = "RoomType ID không được để trống")
    @Positive(message = "RoomType ID phải là số dương")
    private Long roomTypeId;

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

    // ✅ NEW FIELDS FOR DEPOSIT PAYMENT
    @DecimalMin(value = "10.0", message = "Tỷ lệ cọc tối thiểu 10%")
    @DecimalMax(value = "50.0", message = "Tỷ lệ cọc tối đa 50%")
    private Double depositPercentage;

    private Boolean isDepositPayment = false;

    // ✅ NEW HELPER METHODS - FIXED
    public boolean hasSpecificRoomSelected() {
        return roomId != null;
    }

    public boolean wantsDepositPayment() {
        return Boolean.TRUE.equals(isDepositPayment) && depositPercentage != null && depositPercentage > 0;
    }

    public BigDecimal getDepositAmount() {
        if (wantsDepositPayment() && totalPrice != null) {
            BigDecimal total = BigDecimal.valueOf(totalPrice);
            BigDecimal percentage = BigDecimal.valueOf(depositPercentage / 100.0);
            return total.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
        }
        return totalPrice != null ? BigDecimal.valueOf(totalPrice) : BigDecimal.ZERO;
    }

    public BigDecimal getRemainingAmount() {
        if (wantsDepositPayment() && totalPrice != null) {
            BigDecimal total = BigDecimal.valueOf(totalPrice);
            return total.subtract(getDepositAmount()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // ✅ EXISTING VALIDATION METHODS
    @AssertTrue(message = "Ngày check-out phải sau ngày check-in")
    public boolean isCheckOutAfterCheckIn() {
        if (checkInDate == null || checkOutDate == null) {
            return true;
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

    // ✅ NEW VALIDATION
    @AssertTrue(message = "Nếu chọn đặt cọc, phải có tỷ lệ cọc hợp lệ (10-50%)")
    public boolean isValidDepositSettings() {
        if (Boolean.TRUE.equals(isDepositPayment)) {
            return depositPercentage != null && depositPercentage >= 10.0 && depositPercentage <= 50.0;
        }
        return true; // OK if not deposit payment
    }
}