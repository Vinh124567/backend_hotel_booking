package com.example.demo.dto.hotel_image;

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
public class HotelImageResponse {
    private Long id;
    private Long hotelId;
    private String imageUrl;
    private String caption;
    private Boolean isPrimary;
    private String hotelName;
}