package com.example.demo.service.room_type;

import com.example.demo.dto.room_type.RoomTypeRequest;
import com.example.demo.entity.RoomType;

public interface RoomTypeService {
    public RoomType createRoomType(RoomTypeRequest request);

}
