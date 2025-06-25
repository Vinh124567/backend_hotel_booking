package com.example.demo.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOverviewDto {
    private Long totalHotels;
    private Long totalUsers;
    private Long todayBookings;
    private BigDecimal todayRevenue;
    private Long pendingBookings;
    private Long pendingPayments;
}