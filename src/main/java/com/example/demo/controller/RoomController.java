package com.example.demo.controller;

import com.example.demo.dto.room.RoomInvalidRequest;
import com.example.demo.dto.room.RoomRequest;
import com.example.demo.dto.room.RoomResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.booking.BookingAvailabilityService;
import com.example.demo.service.room.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/v1/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final BookingAvailabilityService bookingAvailabilityService;

    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestBody RoomRequest request) {
        roomService.createRoom(request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Tạo phòng thành công");
        response.setMessage("Thêm phòng mới thành công");
        response.setCode(HttpStatus.CREATED.value());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @RequestBody RoomRequest request) {
        roomService.updateRoom(id, request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Cập nhật phòng thành công");
        response.setMessage("Cập nhật thông tin phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Xóa phòng thành công");
        response.setMessage("Xóa phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoom(@PathVariable Long id) {
        RoomResponse room = roomService.getRoom(id);

        ApiResponse<RoomResponse> response = new ApiResponse<>();
        response.setResult(room);
        response.setMessage("Lấy thông tin phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllRooms() {
        List<RoomResponse> rooms = roomService.getAllRooms();

        ApiResponse<List<RoomResponse>> response = new ApiResponse<>();
        response.setResult(rooms);
        response.setMessage("Lấy danh sách phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // API mới - Lấy phòng theo room type ID
    @GetMapping("/room-type/{roomTypeId}")
    public ResponseEntity<?> getRoomsByRoomTypeId(@PathVariable Long roomTypeId) {
        List<RoomResponse> rooms = roomService.getRoomsByRoomTypeId(roomTypeId);

        ApiResponse<List<RoomResponse>> response = new ApiResponse<>();
        response.setResult(rooms);
        response.setMessage("Lấy danh sách phòng theo loại phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // API bổ sung - Lấy phòng trống theo room type ID
    @GetMapping("/room-type/{roomTypeId}/available")
    public ResponseEntity<?> getAvailableRoomsByRoomTypeId(@PathVariable Long roomTypeId) {
        List<RoomResponse> rooms = roomService.getRoomsByRoomTypeId(roomTypeId);

        // Filter chỉ lấy phòng trống
        List<RoomResponse> availableRooms = rooms.stream()
                .filter(room -> Boolean.TRUE.equals(room.getIsAvailable()))
                .collect(java.util.stream.Collectors.toList());

        ApiResponse<List<RoomResponse>> response = new ApiResponse<>();
        response.setResult(availableRooms);
        response.setMessage("Lấy danh sách phòng trống theo loại phòng thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/roomInvalid")
    public ResponseEntity<?> checkRoomsInvalid(@RequestBody RoomInvalidRequest request) {
        boolean isAvailable = bookingAvailabilityService
                .isSpecificRoomAvailable(request.getRoomId(), request.getCheckIn(), request.getCheckOut());

        ApiResponse<Boolean> response = new ApiResponse<>();
        response.setCode(HttpStatus.OK.value());
        response.setMessage("Kiểm tra phòng thành công");
        response.setResult(isAvailable);

        return ResponseEntity.ok(response);
    }



}