package com.example.demo.controller;

import com.example.demo.dto.notification.AdminNotificationResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.notification.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    private final AdminNotificationService adminNotificationService;

    /**
     * Lấy tất cả notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<AdminNotificationResponse>>> getAllNotifications() {
        log.info("Getting all admin notifications");

        List<AdminNotificationResponse> notifications = adminNotificationService.getAllNotifications();

        ApiResponse<List<AdminNotificationResponse>> response = new ApiResponse<>();
        response.setResult(notifications);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy danh sách thông báo thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy notifications chưa đọc
     */
    @GetMapping("/notifications/unread")
    public ResponseEntity<ApiResponse<List<AdminNotificationResponse>>> getUnreadNotifications() {
        log.info("Getting unread admin notifications");

        List<AdminNotificationResponse> notifications = adminNotificationService.getUnreadNotifications();

        ApiResponse<List<AdminNotificationResponse>> response = new ApiResponse<>();
        response.setResult(notifications);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy thông báo chưa đọc thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * Đếm số notifications chưa đọc
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationCount() {
        log.info("Getting unread notification count");

        long count = adminNotificationService.getUnreadCount();

        ApiResponse<Long> response = new ApiResponse<>();
        response.setResult(count);
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Lấy số lượng thông báo chưa đọc thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * Đánh dấu notification đã đọc
     */
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationAsRead(@PathVariable Long id) {
        log.info("Marking notification as read: {}", id);

        adminNotificationService.markAsRead(id);

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Đánh dấu đã đọc thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * Đánh dấu tất cả notifications đã đọc
     */
    @PutMapping("/notifications/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead() {
        log.info("Marking all notifications as read");

        adminNotificationService.markAllAsRead();

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Đánh dấu tất cả đã đọc thành công");

        return ResponseEntity.ok(response);
    }
}