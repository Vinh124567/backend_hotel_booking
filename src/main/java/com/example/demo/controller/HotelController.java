package com.example.demo.controller;

import com.example.demo.dto.hotel.HotelRequest;
import com.example.demo.dto.hotel.HotelResponse;
import com.example.demo.entity.Hotel;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.hotel.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<?> getHotelById(@PathVariable Long id) {
        HotelResponse hotel = hotelService.getHotelById(id);

        ApiResponse<HotelResponse> response = new ApiResponse<>();
        response.setResult(hotel);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/getBasic")
    public ResponseEntity<?> getAllHotelsBasic() {
        List<HotelResponse> hotels = hotelService.getAllHotelsBasic();

        ApiResponse<List<HotelResponse>> response = new ApiResponse<>();
        response.setResult(hotels);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<?> filterHotels(
            @RequestParam(required = false) String cityName,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) List<Long> amenityIds,
            @RequestParam(required = false) Integer numberOfGuests) {

        List<HotelResponse> hotels = hotelService.filterHotels(cityName, minPrice, maxPrice, minRating, amenityIds,numberOfGuests);

        ApiResponse<List<HotelResponse>> response = new ApiResponse<>();
        response.setResult(hotels);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
