package com.example.demo.dto.room_type;


import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class RoomTypeResponse {
    private Long id;
    private String typeName;
    private String description;
    private Integer maxOccupancy;
    private BigDecimal basePrice;
    private BigDecimal sizeSqm;
    private String bedType;
    private Long hotelId;
    private Set<String> imageUrls;
    private Set<String> amenityNames;
}
