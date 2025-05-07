package com.example.demo.service.hotel;
import com.example.demo.dto.hotel.HotelRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.utils.ImageUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final LocationRepository locationRepository;
    private final AmenityRepository amenityRepository;
    private final HotelImageRepository hotelImageRepository;
    private final ModelMapper modelMapper;

    @Override
    public Hotel createHotel(HotelRequest request) {
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy location với ID: " + request.getLocationId()));

        Hotel hotel = modelMapper.map(request, Hotel.class);
        hotel.setLocation(location);

        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            hotel.setAmenities(amenities);
        }

        Hotel savedHotel = hotelRepository.save(hotel);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            Set<HotelImage> hotelImages = request.getImages().stream()
                    .map(imageRequest -> {
                        HotelImage image = new HotelImage();
                        image.setImageUrl(imageRequest.getImageUrl());
                        image.setHotel(savedHotel);
                        return image;
                    })
                    .collect(Collectors.toSet());

            hotelImageRepository.saveAll(hotelImages);
            savedHotel.setImages(hotelImages);
        }
        return savedHotel;
    }

    @Override
    public void deleteHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + id));
        hotelRepository.delete(hotel);
    }

    @Override
    public Hotel updateHotel(Long id, HotelRequest request) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + id));

        // Cập nhật các trường thông tin cơ bản
        modelMapper.map(request, hotel);

        // Cập nhật location
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy location với ID: " + request.getLocationId()));
        hotel.setLocation(location);

        // Cập nhật amenities
        if (request.getAmenityIds() != null) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            hotel.setAmenities(amenities);
        }

        // Cập nhật hình ảnh (xóa cũ - thêm mới với xử lý base64)
        if (request.getImages() != null) {
            // Xóa ảnh cũ
            hotelImageRepository.deleteAll(hotel.getImages());

            // Lưu ảnh mới từ base64
            Set<HotelImage> hotelImages = request.getImages().stream()
                    .map(imageRequest -> {
                        HotelImage image = new HotelImage();

                        // Lưu ảnh và lấy URL
                        String imageUrl = ImageUtils.saveBase64Image(imageRequest.getImageUrl());
                        image.setImageUrl(imageUrl);

                        image.setHotel(hotel);
                        return image;
                    })
                    .collect(Collectors.toSet());

            hotelImageRepository.saveAll(hotelImages);
            hotel.setImages(hotelImages);
        }

        return hotelRepository.save(hotel);
    }



}
