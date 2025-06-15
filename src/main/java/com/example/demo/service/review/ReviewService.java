package com.example.demo.service.review;

import com.example.demo.dto.review.ReviewRequest;
import com.example.demo.dto.review.ReviewResponse;

import java.util.List;
import java.util.Map;

public interface ReviewService {
    void createReview(ReviewRequest request);
    void updateReview(Long id, ReviewRequest request);
    void deleteReview(Long id);
    void approveReview(Long id);
    ReviewResponse getReview(Long id);
    List<ReviewResponse> getAllReviews();
    List<ReviewResponse> getReviewsByHotelId(Long hotelId);
    List<ReviewResponse> getApprovedReviewsByHotelId(Long hotelId);
    List<ReviewResponse> getReviewsByUserId(Long userId);
    List<ReviewResponse> getPendingReviews();
    Map<String, Object> getHotelReviewStats(Long hotelId);

    public boolean canUserReviewHotel(Long hotelId);
    public List<Long> getHotelsEligibleForReview();
}