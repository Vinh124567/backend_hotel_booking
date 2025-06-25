package com.example.demo.dto.report;

import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class HotelStatsResponse {

    // ========== TỔNG QUAN ==========
    private BigDecimal totalRevenue;           // Tổng doanh thu
    private Long totalBookings;                // Tổng booking
    private Long completedBookings;            // Booking hoàn thành
    private Long cancelledBookings;            // Booking đã hủy

    // ========== DOANH THU THEO THÁNG ==========
    private List<MonthlyRevenue> monthlyRevenues;

    @Data
    @Builder
    public static class MonthlyRevenue {
        private String month;                  // "2025-06"
        private String displayMonth;           // "Tháng 6/2025"
        private BigDecimal revenue;            // Doanh thu tháng
        private Long bookings;                 // Số booking tháng
    }
}