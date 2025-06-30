// ✅ BookingResponse.java - UPDATED (thêm vào class hiện có)
package com.example.demo.dto.booking;

import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.Payment;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    // ✅ EXISTING FIELDS (giữ nguyên tất cả)
    private Long id;
    private Long userId;
    private Long roomTypeId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;
    private Integer numberOfGuests;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime bookingDate;
    private Double totalPrice;
    private String specialRequests;
    private String status;
    private Long assignedRoomId;

    // Hotel and room info
    private String roomNumber;
    private String roomTypeName;
    private String userName;
    private String userEmail;
    private Long hotelId;
    private String hotelName;
    private String hotelAddress;
    private BigDecimal hotelStarRating;
    private String hotelPhoneNumber;
    private String hotelEmail;
    private String hotelWebsite;
    private Double hotelAverageRating;
    private Integer hotelReviewCount;
    private List<HotelImageResponse> hotelImages;
    private Double roomTypeBasePrice;
    private Integer roomTypeMaxOccupancy;
    private String roomTypeDescription;
    private Double roomTypeBedSize;
    private String roomTypeAmenities;

    // Payment info
    private Long paymentId;
    private String paymentStatus;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private Boolean paymentExpired;
    private String hotelLocationCity;
    private String hotelLocationDistrict;
    private String primaryHotelImageUrl;
    private Boolean isHotelActive;
    private Hotel.PropertyType hotelPropertyType;
    private Boolean isPaid;
    private String qrCode;

    // Review info
    private Boolean canReview;
    private Boolean hasReviewed;
    private Long existingReviewId;

    // Permission flags
    private Boolean canCancel;
    private Boolean canModify;
    private Boolean canCheckIn;
    private Boolean canCheckOut;

    // ✅ NEW DEPOSIT-RELATED FIELDS
    private Payment.PaymentType paymentType;
    private Double depositAmount;
    private Double remainingAmount;
    private Double depositPercentage;
    private Boolean canPayRemaining;
    private Boolean isFullyPaid;
    private String paymentTypeDisplay;
    private Boolean isDepositPayment;

    // ✅ NEW HELPER METHODS FOR DEPOSIT
    public Boolean getCanPayRemaining() {
        return BookingStatus.DEPOSIT_PAID.equals(status) &&
                remainingAmount != null && remainingAmount > 0;
    }

    public Boolean getIsFullyPaid() {
        return remainingAmount == null || remainingAmount <= 0 ||
                BookingStatus.CONFIRMED.equals(status) ||
                BookingStatus.CHECKED_IN.equals(status) ||
                BookingStatus.COMPLETED.equals(status);
    }

    public Boolean getIsDepositPayment() {
        return Payment.PaymentType.COC_TRUOC.equals(paymentType) ||
                BookingStatus.DEPOSIT_PAID.equals(status);
    }

    public String getPaymentTypeDisplay() {
        if (BookingStatus.DEPOSIT_PAID.equals(status) && depositPercentage != null) {
            return String.format("Đã cọc %.0f%% (%.0f VNĐ)", depositPercentage, depositAmount);
        } else if (getIsFullyPaid()) {
            return "Đã thanh toán đầy đủ";
        } else {
            return "Chưa thanh toán";
        }
    }

    public String getPaymentStatusSummary() {
        if (getIsDepositPayment() && !getIsFullyPaid()) {
            return String.format("Còn lại: %.0f VNĐ", remainingAmount);
        } else if (getIsFullyPaid()) {
            return "Hoàn tất";
        } else {
            return "Chờ thanh toán";
        }
    }

    // ✅ EXISTING COMPUTED PROPERTIES (giữ nguyên)
    public Integer getNumberOfNights() {
        if (checkInDate != null && checkOutDate != null) {
            return (int) checkInDate.until(checkOutDate).getDays();
        }
        return null;
    }

    public Double getPricePerNight() {
        if (totalPrice != null && getNumberOfNights() != null && getNumberOfNights() > 0) {
            return totalPrice / getNumberOfNights();
        }
        return null;
    }

    // ✅ EXISTING STATUS METHODS (giữ nguyên)
    public boolean isPaid() {
        return "Đã xác nhận".equals(status) || "PAID".equals(status);
    }

    public boolean isConfirmed() {
        return "Đã xác nhận".equals(status) || "CONFIRMED".equals(status);
    }

    public boolean isCancelled() {
        return "Đã hủy".equals(status) || "CANCELLED".equals(status);
    }

    public boolean isCheckedIn() {
        return "Đã nhận phòng".equals(status) || "CHECKED_IN".equals(status);
    }

    public boolean isCheckedOut() {
        return "Hoàn thành".equals(status) || "CHECKED_OUT".equals(status);
    }

    public boolean isPending() {
        return "Chờ xác nhận".equals(status) || "PENDING".equals(status);
    }

    public boolean isDepositPaid() {
        return BookingStatus.DEPOSIT_PAID.equals(status);
    }

    // ✅ OTHER EXISTING METHODS (giữ nguyên tất cả)...
}