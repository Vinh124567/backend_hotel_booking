package com.example.demo.dto.amenity;

import com.example.demo.enumm.AmenityCategory;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AmenityResponse {
    private Long id;
    private String amenityName;
    private String amenityType;
    private String iconUrl;
    private AmenityCategory category;
    private String categoryDisplayName;
    private String iconCode;
    private String description;
}