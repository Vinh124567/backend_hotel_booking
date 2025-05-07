package com.example.demo.service.amenity;

import com.example.demo.dto.amenity.AmenityRequest;
import com.example.demo.dto.room.RoomRequest;

public interface AmenityService {
    void createAmenity(AmenityRequest request);
    void updateAmenity(Long id, AmenityRequest request);
    public void deleteAmenity(Long id);

}
