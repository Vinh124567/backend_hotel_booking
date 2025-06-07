package com.example.demo.controller;

import com.example.demo.dto.hotel.HotelResponse;
import com.example.demo.entity.Hotel;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.Favorite.FavoriteService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@AllArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<?> addFavorite(@RequestBody Map<String, Long> request) {
        Long hotelId = request.get("hotelId");
        if (hotelId == null) {
            ApiResponse<String> errorResponse = new ApiResponse<>();
            errorResponse.setMessage("Thiếu thông tin khách sạn");
            errorResponse.setCode(HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.badRequest().body(errorResponse);
        }

        favoriteService.addFavorite(hotelId);

        ApiResponse<String> response = new ApiResponse<>();
        response.setMessage("Đã thêm khách sạn vào danh sách yêu thích");
        response.setCode(HttpStatus.OK.value());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{hotelId}")
    public ResponseEntity<?> removeFavorite(@PathVariable Long hotelId) {
        favoriteService.removeFavorite(hotelId);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Đã xóa khách sạn khỏi danh sách yêu thích");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{hotelId}")
    public ResponseEntity<?> isFavorite(@PathVariable Long hotelId) {
        boolean isFavorite = favoriteService.isFavorite(hotelId);

        ApiResponse<Boolean> response = new ApiResponse<>();
        response.setResult(isFavorite);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getFavoriteHotels() {
        List<HotelResponse> favoriteHotels = favoriteService.getFavoriteHotels();

        ApiResponse<List<HotelResponse>> response = new ApiResponse<>();
        response.setResult(favoriteHotels);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }
}