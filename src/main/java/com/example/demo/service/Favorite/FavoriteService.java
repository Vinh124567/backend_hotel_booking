package com.example.demo.service.Favorite;

import com.example.demo.dto.hotel.HotelResponse;
import com.example.demo.entity.Hotel;

import java.util.List;

public interface FavoriteService {

    // Thêm khách sạn vào danh sách yêu thích
    void addFavorite(Long hotelId);

    // Xóa khách sạn khỏi danh sách yêu thích
    void removeFavorite(Long hotelId);

    // Kiểm tra khách sạn có trong danh sách yêu thích không
    boolean isFavorite(Long hotelId);

    // Lấy danh sách khách sạn yêu thích của người dùng hiện tại
    public List<HotelResponse> getFavoriteHotels() ;}