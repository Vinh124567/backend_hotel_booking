package com.example.demo.controller;

import com.example.demo.dto.room_image.RoomImageRequest;
import com.example.demo.entity.RoomImage;
import com.example.demo.response.ApiResponse;
import com.example.demo.repository.RoomImageRepository;
import com.example.demo.service.roomImage.RoomImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/room-images")
@RequiredArgsConstructor
public class RoomImageController {

    private final RoomImageService roomImageService;
    private final RoomImageRepository roomImageRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createRoomImage(@RequestBody RoomImageRequest request) {
        roomImageService.createRoomImage(request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult("Room image created successfully");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateRoomImage(
            @PathVariable Long id,
            @RequestBody RoomImageRequest request
    ) {
        roomImageService.updateRoomImage(id, request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult("Room image updated successfully");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRoomImage(@PathVariable Long id) {
        roomImageService.deleteRoomImage(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult("Xóa ảnh phòng thành công");

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomImage>>> getAllRoomImages() {
        List<RoomImage> images = roomImageRepository.findAll();

        ApiResponse<List<RoomImage>> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult(images);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomImage>> getRoomImageById(@PathVariable Long id) {
        RoomImage image = roomImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room image not found"));

        ApiResponse<RoomImage> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setResult(image);

        return ResponseEntity.ok(response);
    }
}
