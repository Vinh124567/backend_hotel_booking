package com.example.demo.service.amenity;

import com.example.demo.dto.amenity.AmenityRequest;
import com.example.demo.dto.amenity.AmenityResponse;
import com.example.demo.dto.room.RoomRequest;

import java.util.List;

public interface AmenityService {
    void createAmenity(AmenityRequest request);

    void updateAmenity(Long id, AmenityRequest request);

    public void deleteAmenity(Long id);
    List<AmenityResponse> getAllAmenities();
    List<AmenityResponse> getAmenitiesByHotelId(Long hotelId);

}
