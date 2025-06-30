package com.example.demo.service.notification;

import com.example.demo.dto.notification.AdminNotificationResponse;
import com.example.demo.entity.AdminNotification;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.User;
import com.example.demo.repository.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;

    @Transactional
    public void notifyBookingCancellation(Booking booking, User cancelledBy) {
        if (requiresNotification(booking.getStatus())) {
            AdminNotification notification = AdminNotification.builder()
                    .bookingId(booking.getId())
                    .notificationType(AdminNotification.NotificationType.HUY_BOOKING)
                    .title("🚨 Booking đã bị hủy")
                    .message(buildCancellationMessage(booking, cancelledBy))
                    .priority(AdminNotification.Priority.CAO)
                    .userName(cancelledBy.getFullName())
                    .hotelName(booking.getRoomType().getHotel().getHotelName())
                    .roomType(booking.getRoomType().getTypeName())
                    .checkInDate(booking.getCheckInDate())
                    .totalAmount(booking.getTotalPrice())
                    .build();

            adminNotificationRepository.save(notification);
            log.info("Created admin notification for booking cancellation: {}", booking.getId());
        }
    }

    @Transactional
    public void notifyDepositPayment(Booking booking, Payment payment) {
        AdminNotification notification = AdminNotification.builder()
                .bookingId(booking.getId())
                .notificationType(AdminNotification.NotificationType.DAT_COC)
                .title("🏦 Khách đã đặt cọc")
                .message(buildDepositMessage(booking, payment))
                .priority(AdminNotification.Priority.TRUNG_BINH)
                .userName(booking.getUser().getFullName())
                .hotelName(booking.getRoomType().getHotel().getHotelName())
                .roomType(booking.getRoomType().getTypeName())
                .checkInDate(booking.getCheckInDate())
                .totalAmount(payment.getAmount())
                .build();

        adminNotificationRepository.save(notification);
        log.info("Created admin notification for deposit payment: {}", booking.getId());
    }

    @Transactional
    public void notifyFullPayment(Booking booking, Payment payment) {
        AdminNotification notification = AdminNotification.builder()
                .bookingId(booking.getId())
                .notificationType(AdminNotification.NotificationType.THANH_TOAN_DAY_DU)
                .title("💰 Khách đã thanh toán đầy đủ")
                .message(buildFullPaymentMessage(booking, payment))
                .priority(AdminNotification.Priority.TRUNG_BINH)
                .userName(booking.getUser().getFullName())
                .hotelName(booking.getRoomType().getHotel().getHotelName())
                .roomType(booking.getRoomType().getTypeName())
                .checkInDate(booking.getCheckInDate())
                .totalAmount(payment.getAmount())
                .build();

        adminNotificationRepository.save(notification);
        log.info("Created admin notification for full payment: {}", booking.getId());
    }

    @Transactional
    public void notifyCheckIn(Booking booking) {
        AdminNotification notification = AdminNotification.builder()
                .bookingId(booking.getId())
                .notificationType(AdminNotification.NotificationType.CHECK_IN)
                .title("🏨 Khách đã check-in")
                .message(buildCheckInMessage(booking))
                .priority(AdminNotification.Priority.THAP)
                .userName(booking.getUser().getFullName())
                .hotelName(booking.getRoomType().getHotel().getHotelName())
                .roomType(booking.getRoomType().getTypeName())
                .checkInDate(booking.getCheckInDate())
                .totalAmount(booking.getTotalPrice())
                .build();

        adminNotificationRepository.save(notification);
        log.info("Created admin notification for check-in: {}", booking.getId());
    }

    public List<AdminNotificationResponse> getUnreadNotifications() {
        List<AdminNotification> notifications = adminNotificationRepository
                .findUnreadNotificationsOrderByPriorityAndDate();

        return notifications.stream()
                .map(AdminNotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AdminNotificationResponse> getAllNotifications() {
        List<AdminNotification> notifications = adminNotificationRepository
                .findAllOrderByCreatedAtDesc();

        return notifications.stream()
                .map(AdminNotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public long getUnreadCount() {
        return adminNotificationRepository.countByIsReadFalse();
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        adminNotificationRepository.markAsRead(notificationId);
        log.info("Marked notification as read: {}", notificationId);
    }

    @Transactional
    public void markAllAsRead() {
        adminNotificationRepository.markAllAsRead();
        log.info("Marked all notifications as read");
    }

    // Helper methods
    // ✅ THÊM "Đã thanh toán" để notify khi khách hủy booking đã cọc
    private boolean requiresNotification(String status) {
        return "Đã xác nhận".equals(status) ||
                "Đã đặt cọc".equals(status) ||
                "Đã thanh toán".equals(status) ||      // ✅ THÊM DÒNG NÀY
                "Đã nhận phòng".equals(status);
    }

    private String buildCancellationMessage(Booking booking, User user) {
        return String.format(
                "Booking #%d của khách %s tại %s (phòng %s) cho ngày %s đã bị hủy.\n" +
                        "Trạng thái trước đó: %s\n" +
                        "Tổng tiền: %,.0f VNĐ\n" +
                        "Cần xử lý hoàn tiền và cập nhật phòng trống.",
                booking.getId(),
                user.getFullName(),
                booking.getRoomType().getHotel().getHotelName(),
                booking.getRoomType().getTypeName(),
                booking.getCheckInDate(),
                booking.getStatus(),
                booking.getTotalPrice().doubleValue()
        );
    }

    private String buildDepositMessage(Booking booking, Payment payment) {
        double remaining = booking.getTotalPrice().doubleValue() - payment.getAmount().doubleValue();
        return String.format(
                "Booking #%d: Khách %s đã đặt cọc %,.0f VNĐ cho %s.\n" +
                        "Còn lại: %,.0f VNĐ cần thanh toán trước check-in (%s)\n" +
                        "Tỷ lệ cọc: %.0f%%",
                booking.getId(),
                booking.getUser().getFullName(),
                payment.getAmount().doubleValue(),
                booking.getRoomType().getHotel().getHotelName(),
                remaining,
                booking.getCheckInDate(),
                payment.getDepositPercentage() != null ? payment.getDepositPercentage().doubleValue() : 0
        );
    }

    private String buildFullPaymentMessage(Booking booking, Payment payment) {
        return String.format(
                "Booking #%d: Khách %s đã thanh toán đầy đủ %,.0f VNĐ cho %s.\n" +
                        "Check-in: %s\n" +
                        "Phòng: %s",
                booking.getId(),
                booking.getUser().getFullName(),
                payment.getAmount().doubleValue(),
                booking.getRoomType().getHotel().getHotelName(),
                booking.getCheckInDate(),
                booking.getRoomType().getTypeName()
        );
    }

    private String buildCheckInMessage(Booking booking) {
        return String.format(
                "Booking #%d: Khách %s đã check-in tại %s.\n" +
                        "Phòng: %s\n" +
                        "Check-out dự kiến: %s",
                booking.getId(),
                booking.getUser().getFullName(),
                booking.getRoomType().getHotel().getHotelName(),
                booking.getAssignedRoom() != null ? booking.getAssignedRoom().getRoomNumber() : "Chưa assign",
                booking.getCheckOutDate()
        );
    }

    // ✅ THÊM VÀO AdminNotificationService.java

    @Transactional
    public void notifyNoShow(Booking booking) {
        AdminNotification notification = AdminNotification.builder()
                .bookingId(booking.getId())
                .notificationType(AdminNotification.NotificationType.NO_SHOW)
                .title("🚫 Khách không đến (No-Show)")
                .message(buildNoShowMessage(booking))
                .priority(AdminNotification.Priority.CAO)
                .userName(booking.getUser().getFullName())
                .hotelName(booking.getRoomType().getHotel().getHotelName())
                .roomType(booking.getRoomType().getTypeName())
                .checkInDate(booking.getCheckInDate())
                .totalAmount(booking.getTotalPrice())
                .build();

        adminNotificationRepository.save(notification);
        log.info("Created admin notification for no-show: {}", booking.getId());
    }

    // ✅ THÊM helper method
    private String buildNoShowMessage(Booking booking) {
        return String.format(
                "Booking #%d: Khách %s không đến check-in (%s).\n" +
                        "Khách sạn: %s\n" +
                        "Phòng: %s\n" +
                        "Đã cọc: %,.0f VNĐ\n" +
                        "Còn lại chưa thanh toán: %,.0f VNĐ\n" +
                        "Cần xử lý chính sách hoàn tiền và cập nhật phòng trống.",
                booking.getId(),
                booking.getUser().getFullName(),
                booking.getCheckInDate(),
                booking.getRoomType().getHotel().getHotelName(),
                booking.getRoomType().getTypeName(),
                booking.getDepositAmount() != null ? booking.getDepositAmount().doubleValue() : 0,
                booking.getRemainingAmount() != null ? booking.getRemainingAmount().doubleValue() : 0
        );
    }
}