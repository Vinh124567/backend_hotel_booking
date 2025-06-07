package com.example.demo.dto.amenity;

import com.example.demo.enumm.AmenityCategory;
import lombok.Data;

@Data
public class AmenityRequest {
    private String amenityName;
    private String amenityType;
    private String iconUrl;
    private AmenityCategory category;
    private String iconCode;
    private String description;
}