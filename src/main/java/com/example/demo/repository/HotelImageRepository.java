package com.example.demo.repository;

import com.example.demo.entity.HotelImage;
import com.example.demo.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
}
