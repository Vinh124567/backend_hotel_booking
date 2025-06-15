package com.example.demo.dto.room;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RoomInvalidRequest {
    private Long roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Long excludeBookingId;
}
