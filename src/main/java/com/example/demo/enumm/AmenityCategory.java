package com.example.demo.enumm;

public enum AmenityCategory {
    FOOD_AND_DRINK("Ẩm thực & Đồ uống"),
    TRANSPORTATION("Giao thông & Đi lại"),
    GENERAL("Tiện nghi chung"),
    HOTEL_SERVICE("Dịch vụ khách sạn"),
    BUSINESS_FACILITIES("Tiện nghi doanh nhân"),
    NEARBY_FACILITIES("Tiện ích lân cận"),
    KIDS("Dành cho trẻ em"),
    CONNECTIVITY("Kết nối internet"),
    PUBLIC_FACILITIES("Tiện nghi công cộng");

    private final String displayName;

    AmenityCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}