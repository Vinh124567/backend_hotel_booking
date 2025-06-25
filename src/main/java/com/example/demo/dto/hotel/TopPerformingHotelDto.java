package com.example.demo.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// TopPerformingHotelDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopPerformingHotelDto {
    private Long hotelId;
    private String hotelName;
    private String location;
    private BigDecimal todayRevenue;
    private BigDecimal monthlyRevenue;
    private Long totalBookings;
    private Long todayBookings;
    private Double occupancyRate;
    private Double averageRating;
    private Long totalRooms;
    private Long availableRooms;
    private String status;
}

