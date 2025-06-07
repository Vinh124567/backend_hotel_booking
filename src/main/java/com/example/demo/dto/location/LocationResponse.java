package com.example.demo.dto.location;

import com.example.demo.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {
    private Long id;
    private Long hotelId;
    private String cityName;
    private String province;
    private String country;
    private String description;
    private String imageUrl;
    private Position position;
}