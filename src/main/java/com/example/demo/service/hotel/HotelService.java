package com.example.demo.service.hotel;

import com.example.demo.dto.hotel.HotelRequest;
import com.example.demo.dto.hotel.HotelResponse;
import com.example.demo.entity.Hotel;

import java.util.List;

public interface HotelService {
    Hotel createHotel(HotelRequest request);

    Hotel updateHotel(Long id, HotelRequest request);

    void deleteHotel(Long id);

    HotelResponse getHotelById(Long id);

    public List<HotelResponse> getAllHotelsBasic();

    List<HotelResponse> filterHotels(String cityName, Double minPrice, Double maxPrice, Double minRating, List<Long> amenityIds, Integer numberOfGuests);

}
