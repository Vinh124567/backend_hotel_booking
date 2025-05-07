package com.example.demo.dto.hotel_image;

import lombok.Data;

@Data
public class HotelImageRequest {
    private Long hotelId;
    private String imageUrl;
    private String caption;
    private Boolean isPrimary;
}

