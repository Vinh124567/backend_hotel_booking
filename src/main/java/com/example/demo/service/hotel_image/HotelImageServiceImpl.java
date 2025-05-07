package com.example.demo.service.hotel_image;

import com.example.demo.dto.hotel_image.HotelImageRequest;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.HotelImage;
import com.example.demo.repository.HotelImageRepository;
import com.example.demo.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static com.example.demo.utils.ImageUtils.saveBase64Image;

@Service
@RequiredArgsConstructor
public class HotelImageServiceImpl implements HotelImageService {

    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final ModelMapper modelMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void createHotelImage(HotelImageRequest request) {
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn"));

        String imageUrl = saveBase64Image(request.getImageUrl());
        HotelImage image = modelMapper.map(request, HotelImage.class);
        image.setHotel(hotel);
        image.setImageUrl(imageUrl);

        hotelImageRepository.save(image);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void updateHotelImage(Long id, HotelImageRequest request) {
        HotelImage existingImage = hotelImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn"));

        String imageUrl = saveBase64Image(request.getImageUrl());
        modelMapper.map(request, existingImage);
        existingImage.setHotel(hotel);
        existingImage.setImageUrl(imageUrl);

        hotelImageRepository.save(existingImage);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void deleteHotelImage(Long id) {
        HotelImage image = hotelImageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));
        hotelImageRepository.delete(image);
    }

}
