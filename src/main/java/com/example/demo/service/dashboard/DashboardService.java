package com.example.demo.service.dashboard;


import com.example.demo.dto.dashboard.DashboardOverviewDto;
import com.example.demo.dto.dashboard.RecentBookingDto;
import com.example.demo.dto.hotel.TopPerformingHotelDto;

import java.util.List;

public interface DashboardService {
    DashboardOverviewDto getDashboardOverview();
    List<RecentBookingDto> getRecentBookings();
    List<TopPerformingHotelDto> getTopHotels();
}