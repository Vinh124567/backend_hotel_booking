package com.example.demo.controller;

import com.example.demo.dto.hotel.HotelRequest;
import com.example.demo.entity.Hotel;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.hotel.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/hotel")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @PostMapping("/create")
    public ResponseEntity<?> createHotel(@RequestBody HotelRequest request) {
        Hotel createdHotel = hotelService.createHotel(request);

        ApiResponse<Hotel> response = new ApiResponse<>();
        response.setResult(createdHotel);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateHotel(@PathVariable Long id, @RequestBody HotelRequest request) {
        Hotel updatedHotel = hotelService.updateHotel(id, request);

        ApiResponse<Hotel> response = new ApiResponse<>();
        response.setResult(updatedHotel);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteHotel(@PathVariable Long id) {
        hotelService.deleteHotel(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Xóa khách sạn thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
