package com.example.demo.dto.room;


import lombok.Data;

@Data
public class RoomRequest {
    private Long roomTypeId;
    private String roomNumber;
    private String floor;
    private String status;
}

