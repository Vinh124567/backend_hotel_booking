package com.example.demo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelDashboardStatsResponse {

    // ========== HÔM NAY ==========
    private Long todayCheckIns;            // Check-in hôm nay
    private Long todayCheckOuts;           // Check-out hôm nay
    private Long todayNewBookings;         // Booking mới hôm nay
    private BigDecimal todayRevenue;       // Doanh thu hôm nay

    // ========== THÁNG NÀY ==========
    private Long monthlyBookings;          // Booking tháng này
    private BigDecimal monthlyRevenue;     // Doanh thu tháng này
    private Double monthlyOccupancyRate;   // Tỷ lệ lấp đầy tháng này

    // ========== TRẠNG THÁI PHÒNG ==========
    private Long totalRooms;               // Tổng số phòng
    private Long availableRooms;           // Phòng trống
    private Long occupiedRooms;            // Phòng đã thuê
    private Long maintenanceRooms;         // Phòng bảo trì

    // ========== BOOKING PENDING ==========
    private Long pendingConfirmations;     // Booking chờ xác nhận
    private Long readyForCheckIn;          // Sẵn sàng check-in
    private Long readyForCheckOut;         // Sẵn sàng check-out
    private Long currentlyCheckedIn;       // Đang ở khách sạn

    // ========== SO SÁNH ==========
    private Double revenueGrowthRate;      // % tăng trưởng doanh thu so với tháng trước
    private Double bookingGrowthRate;      // % tăng trưởng booking so với tháng trước
}