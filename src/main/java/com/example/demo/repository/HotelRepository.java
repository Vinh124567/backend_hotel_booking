package com.example.demo.repository;

import com.example.demo.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    // Trong HotelRepository.java
    @Query(value = """
    SELECT h.hotel_id, h.hotel_name, h.address, h.star_rating,
           COUNT(b.booking_id) as total_bookings,
           COALESCE(SUM(CASE WHEN p.payment_status = 'Đã thanh toán' THEN p.amount ELSE 0 END), 0) as total_revenue,
           COALESCE(AVG(rev.rating), 0) as avg_rating
    FROM hotels h
    LEFT JOIN room_types rt ON h.hotel_id = rt.hotel_id  
    LEFT JOIN bookings b ON rt.room_type_id = b.room_type_id
    LEFT JOIN payments p ON b.booking_id = p.booking_id
    LEFT JOIN reviews rev ON h.hotel_id = rev.hotel_id AND rev.is_approved = 1
    WHERE h.is_active = 1
    GROUP BY h.hotel_id, h.hotel_name, h.address, h.star_rating
    ORDER BY total_revenue DESC, total_bookings DESC
    LIMIT 8
    """, nativeQuery = true)
    List<Object[]> findTopHotelsByRevenue();

    // Thêm method tìm hotel active
    List<Hotel> findByIsActiveTrue();
}