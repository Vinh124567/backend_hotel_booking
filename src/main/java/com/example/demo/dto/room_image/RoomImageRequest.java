package com.example.demo.dto.room_image;


import lombok.Data;

@Data
public class RoomImageRequest {
    private Long roomTypeId;
    private String caption;
    private Boolean isPrimary;
    private String imageUrl;
}
