package com.example.demo.service.hotel;

import com.example.demo.dto.hotel.HotelRequest;
import com.example.demo.entity.Hotel;

public interface HotelService {
    Hotel createHotel(HotelRequest request);
    Hotel updateHotel(Long id, HotelRequest request);
    void deleteHotel(Long id);
}
