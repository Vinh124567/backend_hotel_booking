package com.example.demo.service.room_type;

import com.example.demo.dto.room_type.RoomTypeRequest;
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
}
