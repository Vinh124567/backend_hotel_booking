package com.example.demo.dto.room;

import lombok.Data;

@Data
public class RoomResponse {
    private Long id;
    private String roomTypeName;
    private String roomNumber;
    private String floor;
    private String status;
}
