package com.example.demo.repository;

import com.example.demo.entity.RoomType;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotel_Id(Long hotelId);

    @Query("SELECT rt FROM RoomType rt WHERE rt.hotel.id = :hotelId ORDER BY rt.basePrice ASC")
    List<RoomType> findByHotelIdOrderByPrice(@Param("hotelId") Long hotelId);

    @Query("SELECT rt FROM RoomType rt WHERE rt.hotel.id = :hotelId AND rt.maxOccupancy >= :occupancy")
    List<RoomType> findByHotelIdAndMinOccupancy(@Param("hotelId") Long hotelId, @Param("occupancy") Integer occupancy);
}
