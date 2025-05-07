package com.example.demo.controller;
import com.example.demo.dto.hotel_image.HotelImageRequest;
import com.example.demo.entity.HotelImage;
import com.example.demo.repository.HotelImageRepository;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.hotel_image.HotelImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/hotel-images")
@RequiredArgsConstructor
public class HotelImageController {

    private final HotelImageService hotelImageService;
    private final HotelImageRepository hotelImageRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createHotelImage(@RequestBody HotelImageRequest request) {
        hotelImageService.createHotelImage(request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult("Tạo ảnh khách sạn thành công");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateHotelImage(
            @PathVariable Long id,
            @RequestBody HotelImageRequest request
    ) {
        hotelImageService.updateHotelImage(id, request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult("Cập nhật ảnh khách sạn thành công");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteHotelImage(@PathVariable Long id) {
        hotelImageService.deleteHotelImage(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult("Xóa ảnh khách sạn thành công");

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HotelImage>>> getAllHotelImages() {
        List<HotelImage> images = hotelImageRepository.findAll();

        ApiResponse<List<HotelImage>> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult(images);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelImage>> getHotelImageById(@PathVariable Long id) {
        HotelImage image = hotelImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh khách sạn"));

        ApiResponse<HotelImage> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult(image);

        return ResponseEntity.ok(response);
    }
}
