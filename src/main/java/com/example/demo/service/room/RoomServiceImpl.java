package com.example.demo.service.room;

import com.example.demo.dto.room.RoomRequest;


import com.example.demo.dto.room.RoomResponse;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomType;
import com.example.demo.repository.RoomRepository;

import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.service.room.RoomService;

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

        modelMapper.typeMap(Room.class, RoomResponse.class).addMappings(mapper ->
                mapper.map(src -> src.getRoomType().getTypeName(), RoomResponse::setRoomTypeName)
        );

        return modelMapper.map(room, RoomResponse.class);
    }

    @Override
    public List<RoomResponse> getAllRooms() {
        modelMapper.typeMap(Room.class, RoomResponse.class).addMappings(mapper ->
                mapper.map(src -> src.getRoomType().getTypeName(), RoomResponse::setRoomTypeName)
        );

        return roomRepository.findAll()
                .stream()
                .map(room -> modelMapper.map(room, RoomResponse.class))
                .collect(Collectors.toList());
    }

}
