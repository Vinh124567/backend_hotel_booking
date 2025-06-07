package com.example.demo.service.room_type;

import com.example.demo.dto.room_type.RoomTypeRequest;
import com.example.demo.dto.room_type.RoomTypeResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.AmenityRepository;
import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.RoomImageRepository;
import com.example.demo.repository.RoomTypeRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;
    private final AmenityRepository amenityRepository;
    private final RoomImageRepository roomImageRepository;
    private final ModelMapper modelMapper;


    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public RoomType createRoomType(RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel không tồn tại với ID: " + request.getHotelId()));
        RoomType roomType = modelMapper.map(request, RoomType.class);
        roomType.setHotel(hotel);

        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            roomType.setAmenities(amenities);
        }

        RoomType savedRoomType = roomTypeRepository.save(roomType);

        // Gán images
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            Set<RoomImage> images = request.getImageUrls().stream()
                    .map(url -> {
                        RoomImage image = new RoomImage();
                        image.setImageUrl(url);
                        image.setRoomType(savedRoomType);
                        return image;
                    })
                    .collect(Collectors.toSet());

            roomImageRepository.saveAll(images);
            savedRoomType.setImages(images);
        }


        return savedRoomType;
    }

    @Override
    public List<RoomTypeResponse> getRoomTypesByHotelId(Long hotelId) {
        // Kiểm tra hotel có tồn tại không
        hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel không tồn tại với ID: " + hotelId));

        // Lấy danh sách room types theo hotel ID
        List<RoomType> roomTypes = roomTypeRepository.findByHotel_Id(hotelId);

        // Convert sang DTO
        return roomTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private RoomTypeResponse convertToResponse(RoomType roomType) {
        RoomTypeResponse response = modelMapper.map(roomType, RoomTypeResponse.class);

        // Set hotelId
        response.setHotelId(roomType.getHotel().getId());

        // Set image URLs
        if (roomType.getImages() != null && !roomType.getImages().isEmpty()) {
            Set<String> imageUrls = roomType.getImages().stream()
                    .map(RoomImage::getImageUrl)
                    .collect(Collectors.toSet());
            response.setImageUrls(imageUrls);
        }

        // Set amenity names - Sửa từ getName() thành getAmenityName()
        if (roomType.getAmenities() != null && !roomType.getAmenities().isEmpty()) {
            Set<String> amenityNames = roomType.getAmenities().stream()
                    .map(Amenity::getAmenityName)  // Sửa ở đây
                    .collect(Collectors.toSet());
            response.setAmenityNames(amenityNames);
        }

        return response;
    }
}
