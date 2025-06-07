package com.example.demo.dto.hotel;

import com.example.demo.dto.hotel_image.HotelImageRequest;
import com.example.demo.entity.Hotel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
public class HotelRequest {

    private String hotelName;
    private String address;
    private String description;
    private BigDecimal starRating;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String phoneNumber;
    private String email;
    private String website;
    private Boolean isActive;

    private Long locationId;

    private List<Long> amenityIds;

    private List<HotelImageRequest> images;
    private Integer distanceToBeach = 0;
    private String hotelCategory;
    private Hotel.PropertyType propertyType;
    // private List<RoomTypeRequest> roomTypes;

}
