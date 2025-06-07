package com.example.demo.service.room;

import com.example.demo.dto.room.RoomRequest;
import com.example.demo.dto.room.RoomResponse;
import java.util.List;

public interface RoomService {
    void createRoom(RoomRequest request);
    void updateRoom(Long id, RoomRequest request);
    void deleteRoom(Long id);
    RoomResponse getRoom(Long id);
    List<RoomResponse> getAllRooms();
    List<RoomResponse> getRoomsByRoomTypeId(Long roomTypeId);
}