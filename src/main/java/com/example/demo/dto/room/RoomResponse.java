package com.example.demo.dto.room;

import lombok.Data;

@Data
public class RoomResponse {
    private Long id;
    private String roomTypeName;
    private String roomNumber;
    private String floor;
    private String status;
    private Long roomTypeId;
    private Boolean isAvailable;

    // Getter cho isAvailable (backup method)
    public Boolean getIsAvailable() {
        return this.isAvailable != null ? this.isAvailable : "Trống".equals(this.status);
    }
}