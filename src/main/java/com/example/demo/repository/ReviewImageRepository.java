package com.example.demo.repository;

import com.example.demo.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    // Tìm tất cả hình ảnh của một đánh giá
    List<ReviewImage> findByReviewId(Long reviewId);

    // Xóa tất cả hình ảnh của một đánh giá
    void deleteByReviewId(Long reviewId);
}