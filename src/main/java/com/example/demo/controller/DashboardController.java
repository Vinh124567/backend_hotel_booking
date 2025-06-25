package com.example.demo.controller;

import com.example.demo.response.ApiResponse;
import com.example.demo.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<?> getDashboardOverview() {
        var overview = dashboardService.getDashboardOverview();

        ApiResponse<Object> response = new ApiResponse<>();
        response.setResult(overview);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy thông tin dashboard thành công");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent-bookings") // ✅ Giữ endpoint cho Flutter
    public ResponseEntity<?> getRecentBookings() {
        var topHotels = dashboardService.getTopHotels();

        ApiResponse<Object> response = new ApiResponse<>();
        response.setResult(topHotels);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách khách sạn hiệu suất cao thành công"); // ✅ Đổi message

        return ResponseEntity.ok(response);
    }

}