package com.example.demo.controller;

import com.example.demo.dto.room_type.RoomTypeRequest;
import com.example.demo.entity.RoomType;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.room_type.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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



}
