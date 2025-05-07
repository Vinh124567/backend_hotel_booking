package com.example.demo.service.hotel_image;


import com.example.demo.dto.hotel_image.HotelImageRequest;

public interface HotelImageService {
    void createHotelImage(HotelImageRequest request);
    void updateHotelImage(Long id, HotelImageRequest request);
    void deleteHotelImage(Long id);
}

