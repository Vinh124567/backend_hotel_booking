// ✅ AdminNotification.java - NEW ENTITY
package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority = Priority.TRUNG_BINH;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "user_name")
    private String userName;

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "room_type")
    private String roomType;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    // ✅ CẬP NHẬT AdminNotification.NotificationType
    public enum NotificationType {
        HUY_BOOKING("Hủy booking"),
        DAT_COC("Đặt cọc"),
        THANH_TOAN_DAY_DU("Thanh toán đầy đủ"),
        CHECK_IN("Check-in"),
        CHECK_OUT("Check-out"),
        NO_SHOW("Không đến");  // ✅ THÊM DÒNG NÀY

        private final String displayName;

        NotificationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Priority {
        THAP("Thấp"),
        TRUNG_BINH("Trung bình"),
        CAO("Cao");

        private final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }


}