package com.example.demo.controller;

import com.example.demo.dto.amenity.AmenityRequest;
import com.example.demo.dto.amenity.AmenityResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.amenity.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/amenity")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    @PostMapping("/create")
    public ResponseEntity<?> createAmenity(@RequestBody AmenityRequest request) {
        amenityService.createAmenity(request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Thêm tiện nghi thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateAmenity(@PathVariable Long id, @RequestBody AmenityRequest request) {
        amenityService.updateAmenity(id, request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Cập nhật tiện nghi thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAmenity(@PathVariable Long id) {
        amenityService.deleteAmenity(id);
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Xóa tiện nghi thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<?> getAmenitiesByHotelId(@PathVariable Long hotelId) {
        List<AmenityResponse> amenities = amenityService.getAmenitiesByHotelId(hotelId);
        ApiResponse<List<AmenityResponse>> response = new ApiResponse<>();
        response.setResult(amenities);
        response.setCode(HttpStatus.OK.value());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
