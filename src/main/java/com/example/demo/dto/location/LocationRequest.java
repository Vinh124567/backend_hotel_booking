package com.example.demo.dto.location;
import com.example.demo.entity.Position;
import lombok.Data;

@Data
public class LocationRequest {
    private String cityName;
    private String province;
    private String country;
    private String description;
    private String imageUrl;
    private Position position;
}
