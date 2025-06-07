package com.example.demo.dto.booking;
public enum BookingStatus {
    CONFIRMED("CONFIRMED", "Đã xác nhận"),
    PAID("PAID", "Đã thanh toán"),
    CANCELLED("CANCELLED", "Đã hủy"),
    CHECKED_IN("CHECKED_IN", "Đã nhận phòng"),
    CHECKED_OUT("CHECKED_OUT", "Đã trả phòng"),
    NO_SHOW("NO_SHOW", "Không đến"),
    EXPIRED("EXPIRED", "Đã hết hạn");

    private final String code;
    private final String description;

    BookingStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static BookingStatus fromCode(String code) {
        for (BookingStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown booking status code: " + code);
    }
}