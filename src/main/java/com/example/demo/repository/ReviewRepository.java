package com.example.demo.repository;

import com.example.demo.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Tìm tất cả đánh giá của một khách sạn
    List<Review> findByHotelId(Long hotelId);

    // Tìm tất cả đánh giá đã được phê duyệt của một khách sạn
    List<Review> findByHotelIdAndIsApprovedTrue(Long hotelId);

    // Tìm tất cả đánh giá của một người dùng
    List<Review> findByUserId(Long userId);

    // Tìm đánh giá của một người dùng cho một khách sạn cụ thể
    Review findByUserIdAndHotelId(Long userId, Long hotelId);

    // Tìm đánh giá chưa được phê duyệt
    List<Review> findByIsApprovedFalse();

    // Đếm số lượng đánh giá của một khách sạn
    Long countByHotelId(Long hotelId);

    // Tìm kiếm đánh giá theo thang điểm
    @Query("SELECT r FROM Review r WHERE r.hotel.id = :hotelId AND r.rating >= :minRating")
    List<Review> findByHotelIdAndMinRating(@Param("hotelId") Long hotelId, @Param("minRating") Double minRating);

    // Tìm những đánh giá mới nhất
    List<Review> findByHotelIdOrderByReviewDateDesc(Long hotelId);

    // Tìm những đánh giá có chứa ảnh
    @Query("SELECT r FROM Review r JOIN r.images i WHERE r.hotel.id = :hotelId GROUP BY r.id HAVING COUNT(i) > 0")
    List<Review> findReviewsWithImages(@Param("hotelId") Long hotelId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.user.id = :userId AND r.hotel.id = :hotelId")
    boolean existsByUserIdAndHotelId(@Param("userId") Long userId, @Param("hotelId") Long hotelId);
}