package com.example.demo.repository;

import com.example.demo.entity.Amenity;
import com.example.demo.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
}
