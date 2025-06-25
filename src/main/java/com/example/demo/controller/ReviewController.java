package com.example.demo.controller;

import com.example.demo.dto.review.ReviewRequest;
import com.example.demo.dto.review.ReviewResponse;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    public ResponseEntity<?> createReview(@RequestBody ReviewRequest request) {
        reviewService.createReview(request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Thêm đánh giá thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id, @RequestBody ReviewRequest request) {
        reviewService.updateReview(id, request);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Cập nhật đánh giá thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/eligible-hotels")
    public ResponseEntity<?> getEligibleHotels() {
        List<Long> eligibleHotelIds = reviewService.getHotelsEligibleForReview();

        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setResult(Map.of(
                "hotelIds", eligibleHotelIds,
                "count", eligibleHotelIds.size(),
                "message", eligibleHotelIds.isEmpty() ?
                        "Không có khách sạn nào có thể đánh giá" :
                        "Danh sách khách sạn có thể đánh giá"
        ));
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping("/can-review/{hotelId}")
    public ResponseEntity<?> canReviewHotel(@PathVariable Long hotelId) {
        boolean canReview = reviewService.canUserReviewHotel(hotelId);

        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setResult(Map.of(
                "canReview", canReview,
                "message", canReview ? "Có thể đánh giá" : "Không thể đánh giá"
        ));
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Xóa đánh giá thành công");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<?> approveReview(@PathVariable Long id) {
        reviewService.approveReview(id);

        ApiResponse<String> response = new ApiResponse<>();
        response.setResult("Đã phê duyệt đánh giá");
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReview(@PathVariable Long id) {
        ReviewResponse review = reviewService.getReview(id);

        ApiResponse<ReviewResponse> response = new ApiResponse<>();
        response.setResult(review);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllReviews() {
        List<ReviewResponse> reviews = reviewService.getAllReviews();

        ApiResponse<List<ReviewResponse>> response = new ApiResponse<>();
        response.setResult(reviews);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<?> getReviewsByHotelId(@PathVariable Long hotelId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByHotelId(hotelId);

        ApiResponse<List<ReviewResponse>> response = new ApiResponse<>();
        response.setResult(reviews);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/hotel/{hotelId}/approved")
    public ResponseEntity<?> getApprovedReviewsByHotelId(@PathVariable Long hotelId) {
        List<ReviewResponse> reviews = reviewService.getApprovedReviewsByHotelId(hotelId);

        ApiResponse<List<ReviewResponse>> response = new ApiResponse<>();
        response.setResult(reviews);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getReviewsByUserId(@PathVariable Long userId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByUserId(userId);

        ApiResponse<List<ReviewResponse>> response = new ApiResponse<>();
        response.setResult(reviews);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public ResponseEntity<?> getPendingReviews() {
        List<ReviewResponse> reviews = reviewService.getPendingReviews();

        ApiResponse<List<ReviewResponse>> response = new ApiResponse<>();
        response.setResult(reviews);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/hotel/{hotelId}/stats")
    public ResponseEntity<?> getHotelReviewStats(@PathVariable Long hotelId) {
        Map<String, Object> stats = reviewService.getHotelReviewStats(hotelId);

        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setResult(stats);
        response.setCode(HttpStatus.OK.value());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}