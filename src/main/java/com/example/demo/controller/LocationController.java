package com.example.demo.controller;

import com.example.demo.dto.location.LocationRequest;
import com.example.demo.dto.location.LocationResponse;
import com.example.demo.entity.Location;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.location.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<ApiResponse<Location>> createLocation(@RequestBody LocationRequest request) {
        Location created = locationService.createLocation(request);

        ApiResponse<Location> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult(created);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getAllLocations() {
        List<LocationResponse> locations = locationService.getAllLocations();

        ApiResponse<List<LocationResponse>> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult(locations);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Location>> getLocationById(@PathVariable Long id) {
        Location location = locationService.getLocationById(id);

        ApiResponse<Location> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult(location);

        return ResponseEntity.ok(response);
    }
}
