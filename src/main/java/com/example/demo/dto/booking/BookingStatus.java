// ✅ BookingStatus.java - UPDATED
package com.example.demo.dto.booking;

public final class BookingStatus {
    public static final String PAID = "Đã thanh toán";           // ✅ THÊM MỚI

    public static final String TEMPORARY = "Tạm giữ chỗ";
    public static final String PENDING = "Chờ xác nhận";
    public static final String CONFIRMED = "Đã xác nhận";
    public static final String CHECKED_IN = "Đã nhận phòng";
    public static final String COMPLETED = "Hoàn thành";
    public static final String CANCELLED = "Đã hủy";
    public static final String NO_SHOW = "Không đến";

    // ✅ NEW STATUS
    public static final String DEPOSIT_PAID = "Đã đặt cọc";

    private BookingStatus() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isValidStatus(String status) {
        return TEMPORARY.equals(status) ||
                PENDING.equals(status) ||
                CONFIRMED.equals(status) ||
                CHECKED_IN.equals(status) ||
                COMPLETED.equals(status) ||
                CANCELLED.equals(status) ||
                NO_SHOW.equals(status) ||
                DEPOSIT_PAID.equals(status); // ✅ NEW
    }

    public static boolean canBeConfirmed(String status) {
        return TEMPORARY.equals(status) ||
                PENDING.equals(status) ||
                DEPOSIT_PAID.equals(status); // ✅ NEW
    }

    public static boolean isActive(String status) {
        return TEMPORARY.equals(status) ||
                PENDING.equals(status) ||
                CONFIRMED.equals(status) ||
                CHECKED_IN.equals(status) ||
                DEPOSIT_PAID.equals(status); // ✅ NEW
    }

    public static boolean isCancellable(String status) {
        return TEMPORARY.equals(status) ||
                PENDING.equals(status) ||
                CONFIRMED.equals(status) ||
                PAID.equals(status) ||           // ✅ THÊM
                DEPOSIT_PAID.equals(status);
    }

    // ✅ NEW HELPER METHODS
    public static boolean requiresAdminNotification(String status) {
        return CONFIRMED.equals(status) ||
                DEPOSIT_PAID.equals(status) ||
                CHECKED_IN.equals(status);
    }

    public static boolean canPayRemaining(String status) {
        return DEPOSIT_PAID.equals(status);
    }

    public static boolean isPaidStatus(String status) {
        return CONFIRMED.equals(status) ||
                CHECKED_IN.equals(status) ||
                COMPLETED.equals(status);
    }
}