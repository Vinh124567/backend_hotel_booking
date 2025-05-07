package com.example.demo.dto.amenity;


import lombok.Data;

@Data
public class AmenityRequest {
    private String amenityName;
    private String amenityType;
    private String iconUrl;
}
