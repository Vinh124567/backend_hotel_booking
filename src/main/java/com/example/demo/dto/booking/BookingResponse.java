package com.example.demo.dto.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    // Additional fields for detailed response
    private String roomNumber;

    private String roomTypeName;

    private String userName;

    private String userEmail;

    // Calculated fields
    private Integer numberOfNights;

    private Double pricePerNight;

    private Boolean canCancel;

    private Boolean canModify;

    private Boolean canCheckIn;

    private Boolean canCheckOut;

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

    // Status helper methods
    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public boolean isCheckedIn() {
        return "CHECKED_IN".equals(status);
    }

    public boolean isCheckedOut() {
        return "CHECKED_OUT".equals(status);
    }
}
