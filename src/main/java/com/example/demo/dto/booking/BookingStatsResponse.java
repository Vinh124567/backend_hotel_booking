package com.example.demo.dto.booking;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class BookingStatsResponse {
    private Long totalBookings;
    private Long activeBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private Double totalSpent;
    private Long favoriteHotels;
}