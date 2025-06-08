package com.example.demo.dto.booking;

import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.entity.Hotel;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {

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

    // ========== EXISTING FIELDS ==========
    private String roomNumber;
    private String roomTypeName;
    private String userName;
    private String userEmail;

    // ========== ✅ NEW HOTEL INFORMATION FIELDS ==========
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

    // ========== ✅ NEW ROOM TYPE DETAILS ==========
    private Double roomTypeBasePrice;
    private Integer roomTypeMaxOccupancy;
    private String roomTypeDescription;
    private Double roomTypeBedSize;
    private String roomTypeAmenities;

    // ========== EXISTING CALCULATED FIELDS ==========
    private Integer numberOfNights;
    private Double pricePerNight;
    private Boolean canCancel;
    private Boolean canModify;
    private Boolean canCheckIn;
    private Boolean canCheckOut;


    private Long paymentId;
    private String paymentStatus;           // Trạng thái payment thực tế
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private Boolean paymentExpired;
    private String hotelLocationCity;
    private String hotelLocationDistrict;
    private String primaryHotelImageUrl;
    private Boolean isHotelActive;
    private Hotel.PropertyType hotelPropertyType;

    private Boolean isPaid;            // true/false

    // Computed properties for UI
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

    // ========== ✅ NEW HELPER METHODS ==========

    /**
     * Get primary hotel image URL or first available image
     */
    public String getPrimaryHotelImageUrl() {
        if (hotelImages != null && !hotelImages.isEmpty()) {
            // Find primary image first
            return hotelImages.stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(HotelImageResponse::getImageUrl)
                    .findFirst()
                    .orElse(hotelImages.get(0).getImageUrl()); // Fallback to first image
        }
        return null;
    }

    /**
     * Get formatted hotel address
     */
    public String getFormattedHotelAddress() {
        StringBuilder address = new StringBuilder();
        if (hotelAddress != null) {
            address.append(hotelAddress);
        }
        if (hotelLocationDistrict != null) {
            if (address.length() > 0) address.append(", ");
            address.append(hotelLocationDistrict);
        }
        if (hotelLocationCity != null) {
            if (address.length() > 0) address.append(", ");
            address.append(hotelLocationCity);
        }
        return address.toString();
    }

    /**
     * Get hotel rating display (e.g., "4.7")
     */
    public String getHotelRatingDisplay() {
        if (hotelAverageRating != null) {
            return String.format("%.1f", hotelAverageRating);
        }
        return "N/A";
    }

    /**
     * Check if booking is for premium hotel (4+ stars)
     */
    public boolean isPremiumHotel() {
        return false;
    }

    // ========== EXISTING STATUS HELPER METHODS ==========
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
}