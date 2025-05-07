package com.example.demo.service.roomImage;

import com.example.demo.dto.room_image.RoomImageRequest;
import com.example.demo.entity.RoomImage;
import com.example.demo.entity.RoomType;
import com.example.demo.repository.RoomImageRepository;
import com.example.demo.repository.RoomTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static com.example.demo.utils.ImageUtils.saveBase64Image;

@Service
@AllArgsConstructor
public class RoomImageServiceImpl implements RoomImageService {

    private final RoomImageRepository roomImageRepository;
    private final RoomTypeRepository roomTypeRepository;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void createRoomImage(RoomImageRequest request) {
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Room type not found"));

        String imageUrl = saveBase64Image(request.getImageUrl());

        RoomImage image = new RoomImage();
        image.setRoomType(roomType);
        image.setCaption(request.getCaption());
        image.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()));
        image.setImageUrl(imageUrl);

        roomImageRepository.save(image);

    }

    @PreAuthorize("hasRole('USER')")
    @Override
    public void updateRoomImage(Long id, RoomImageRequest request) {
        RoomImage existingImage = roomImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh phòng"));

        String imageUrl = saveBase64Image(request.getImageUrl());

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));

        existingImage.setRoomType(roomType);
        existingImage.setCaption(request.getCaption());
        existingImage.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()));
        existingImage.setImageUrl(imageUrl);

        roomImageRepository.save(existingImage);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void deleteRoomImage(Long id) {
        RoomImage image = roomImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh phòng"));
        roomImageRepository.delete(image);
    }


}
