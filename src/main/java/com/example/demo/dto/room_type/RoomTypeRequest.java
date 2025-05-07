package com.example.demo.dto.room_type;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomTypeRequest {

    private Long hotelId;

    private String typeName;
    private String description;
    private Integer maxOccupancy;
    private BigDecimal basePrice;
    private BigDecimal sizeSqm;
    private String bedType;

    private List<String> imageUrls;
    private List<Long> amenityIds;
}

