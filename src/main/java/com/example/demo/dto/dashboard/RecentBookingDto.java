package com.example.demo.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentBookingDto {
    private Long bookingId;
    private String userName;
    private String hotelName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime bookingDate;
}