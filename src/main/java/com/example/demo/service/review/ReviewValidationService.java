package com.example.demo.service.review;

import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewValidationService {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Kiểm tra user có quyền review hotel này không
     */
    public void validateReviewEligibility(Long userId, Long hotelId) {
        // 1. Kiểm tra user đã có booking completed cho hotel này chưa
        if (!bookingRepository.hasCompletedBookingForHotel(userId, hotelId)) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá khách sạn sau khi đã hoàn thành lưu trú");
        }

        // 2. Kiểm tra user đã review hotel này chưa
        if (reviewRepository.existsByUserIdAndHotelId(userId, hotelId)) {
            throw new RuntimeException("Bạn đã đánh giá khách sạn này rồi. Mỗi khách sạn chỉ được đánh giá một lần");
        }
    }

    /**
     * Kiểm tra user có thể review hotel này không (không throw exception)
     */
    public boolean canReviewHotel(Long userId, Long hotelId) {
        try {
            validateReviewEligibility(userId, hotelId);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Lấy danh sách hotels user có thể review
     */
    public List<Long> getHotelsEligibleForReview(Long userId) {
        return bookingRepository.findHotelsEligibleForReview(userId);
    }
}