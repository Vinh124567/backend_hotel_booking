package com.example.demo.service.location;

import com.example.demo.dto.location.LocationRequest;
import com.example.demo.dto.location.LocationResponse;
import com.example.demo.entity.Location;

import java.util.List;

public interface LocationService {
    Location createLocation(LocationRequest request);

    List<LocationResponse> getAllLocations();

    Location getLocationById(Long id);
}
