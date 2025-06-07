package com.example.demo.service.room;

import com.example.demo.dto.room.RoomRequest;
import com.example.demo.dto.room.RoomResponse;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomType;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final ModelMapper modelMapper;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void createRoom(RoomRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("RoomType not found with ID: " + request.getRoomTypeId()));
        Room room = modelMapper.map(request, Room.class);
        room.setRoomType(roomType);
        roomRepository.save(room);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("RoomType not found with ID: " + request.getRoomTypeId()));

        modelMapper.map(request, room);
        room.setRoomType(roomType);

        roomRepository.save(room);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));
        roomRepository.delete(room);
    }

    @Override
    public RoomResponse getRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));

        return convertToResponse(room);
    }

    @Override
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomResponse> getRoomsByRoomTypeId(Long roomTypeId) {
        // Kiểm tra room type có tồn tại không
        roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("RoomType không tồn tại với ID: " + roomTypeId));

        // Lấy danh sách phòng theo room type ID
        List<Room> rooms = roomRepository.findByRoomType_Id(roomTypeId);

        // Convert sang DTO
        return rooms.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Helper method để convert Room entity sang RoomResponse DTO
    private RoomResponse convertToResponse(Room room) {
        RoomResponse response = modelMapper.map(room, RoomResponse.class);

        // Set room type name
        response.setRoomTypeName(room.getRoomType().getTypeName());

        // Set room type ID
        response.setRoomTypeId(room.getRoomType().getId());

        // Set isAvailable based on status
        response.setIsAvailable("Trống".equals(room.getStatus()));

        return response;
    }
}