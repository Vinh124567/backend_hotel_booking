package com.example.demo.service.room_type;

import com.example.demo.dto.room_type.RoomTypeRequest;
import com.example.demo.dto.room_type.RoomTypeResponse;
import com.example.demo.entity.RoomType;

import java.util.List;

public interface RoomTypeService {
    public RoomType createRoomType(RoomTypeRequest request);
    List<RoomTypeResponse> getRoomTypesByHotelId(Long hotelId);
    RoomType updateRoomType(Long roomTypeId, RoomTypeRequest request);

}
