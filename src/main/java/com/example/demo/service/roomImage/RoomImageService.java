package com.example.demo.service.roomImage;

import com.example.demo.dto.room_image.RoomImageRequest;

public interface RoomImageService {
    void createRoomImage(RoomImageRequest request);

    void updateRoomImage(Long id, RoomImageRequest request);

    void deleteRoomImage(Long id);
}

