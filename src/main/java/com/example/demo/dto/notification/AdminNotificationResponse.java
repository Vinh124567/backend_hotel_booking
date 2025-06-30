// ✅ AdminNotificationResponse.java - NEW DTO
package com.example.demo.dto.notification;

import com.example.demo.entity.AdminNotification;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminNotificationResponse {

    private Long id;
    private Long bookingId;
    private String notificationType;
    private String title;
    private String message;
    private String priority;
    private Boolean isRead;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String userName;
    private String hotelName;
    private String roomType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;

    private BigDecimal totalAmount;

    // Helper methods
    public String getPriorityColor() {
        if ("Cao".equals(priority)) return "danger";
        if ("Trung bình".equals(priority)) return "warning";
        return "info";
    }

    public String getNotificationIcon() {
        switch (notificationType) {
            case "Hủy booking": return "🚨";
            case "Đặt cọc": return "🏦";
            case "Thanh toán đầy đủ": return "💰";
            case "Check-in": return "🏨";
            case "Check-out": return "👋";
            default: return "📢";
        }
    }

    public static AdminNotificationResponse fromEntity(AdminNotification entity) {
        return AdminNotificationResponse.builder()
                .id(entity.getId())
                .bookingId(entity.getBookingId())
                .notificationType(entity.getNotificationType().getDisplayName())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .priority(entity.getPriority().getDisplayName())
                .isRead(entity.getIsRead() != null ? entity.getIsRead() : false) // ✅ SỬA                .createdAt(entity.getCreatedAt())
                .userName(entity.getUserName())
                .hotelName(entity.getHotelName())
                .roomType(entity.getRoomType())
                .checkInDate(entity.getCheckInDate())
                .totalAmount(entity.getTotalAmount())
                .build();
    }
}