package com.example.demo.controller;

import com.example.demo.dto.room_type.RoomTypeRequest;
import com.example.demo.dto.room_type.RoomTypeResponse;
import com.example.demo.entity.RoomType;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.room_type.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/room-type")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @PostMapping("/create")
    public ResponseEntity<?> createRoomType(@RequestBody RoomTypeRequest request) {
        RoomType createdRoomType = roomTypeService.createRoomType(request);

        ApiResponse<RoomType> response = new ApiResponse<>();
        response.setResult(createdRoomType);
        response.setMessage("Thêm loại phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<?> getRoomTypesByHotelId(@PathVariable Long hotelId) {
        List<RoomTypeResponse> roomTypes = roomTypeService.getRoomTypesByHotelId(hotelId);

        ApiResponse<List<RoomTypeResponse>> response = new ApiResponse<>();
        response.setResult(roomTypes);
        response.setMessage("Lấy danh sách loại phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/update/{roomTypeId}")
    public ResponseEntity<?> updateRoomType(@PathVariable Long roomTypeId, @RequestBody RoomTypeRequest request) {
        RoomType updatedRoomType = roomTypeService.updateRoomType(roomTypeId, request);

        ApiResponse<RoomType> response = new ApiResponse<>();
        response.setResult(updatedRoomType);
        response.setMessage("Cập nhật loại phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}