package com.example.demo.repository;

import com.example.demo.entity.Favorite;
import com.example.demo.entity.FavoriteId;
import com.example.demo.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    // Kiểm tra khách sạn có trong danh sách yêu thích không
    boolean existsByUserIdAndHotelId(Long userId, Long hotelId);

    // Xóa khách sạn khỏi danh sách yêu thích
    void deleteByIdUserIdAndIdHotelId(Long userId, Long hotelId);

    // Tìm favorite theo userId và hotelId
    Optional<Favorite> findByIdUserIdAndIdHotelId(Long userId, Long hotelId);

    // Lấy danh sách khách sạn yêu thích của người dùng
    @Query("SELECT f.hotel FROM Favorite f WHERE f.user.id = :userId ORDER BY f.addedDate DESC")
    List<Hotel> findHotelsByUserId(@Param("userId") Long userId);

    // Lấy danh sách favorite của người dùng
    List<Favorite> findByUserIdOrderByAddedDateDesc(Long userId);
}