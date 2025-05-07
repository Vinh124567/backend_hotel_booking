package com.example.demo.controller;


import com.example.demo.dto.room.RoomRequest;
import com.example.demo.dto.room.RoomResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.room.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestBody RoomRequest request) {
        roomService.createRoom(request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Thêm phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody RoomRequest request) {
        roomService.updateRoom(id, request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Cập nhật phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Xóa phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoom(@PathVariable Long id) {
        RoomResponse room = roomService.getRoom(id);

        ApiResponse<RoomResponse> response = new ApiResponse<>();
        response.setResult(room);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllRooms() {
        List<RoomResponse> rooms = roomService.getAllRooms();

        ApiResponse<List<RoomResponse>> response = new ApiResponse<>();
        response.setResult(rooms);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
