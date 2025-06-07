package com.example.demo.dto.hotel;

import com.example.demo.dto.amenity.AmenityResponse;
import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.dto.location.LocationResponse;
import com.example.demo.dto.room_type.RoomTypeResponse; // Thêm import
import com.example.demo.entity.Hotel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelResponse {
    private Long id;
    private String hotelName;
    private String address;
    private String description;
    private BigDecimal starRating;
    private LocationResponse location;
    private String phoneNumber;
    private String email;
    private String website;
    private List<HotelImageResponse> images;
    private Double averageRating;
    private Integer reviewCount;
    private List<AmenityResponse> amenities;
    private Integer distanceToBeach = 0;
    private String hotelCategory;
    private Hotel.PropertyType propertyType;
    private List<RoomTypeResponse> roomTypes;
}