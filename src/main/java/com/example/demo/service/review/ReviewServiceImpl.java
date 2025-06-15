package com.example.demo.service.review;

import com.example.demo.service.review.ReviewValidationService;

import com.example.demo.dto.review.ReviewRequest;
import com.example.demo.dto.review.ReviewResponse;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.Review;
import com.example.demo.entity.ReviewImage;
import com.example.demo.entity.User;
import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.ReviewImageRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ModelMapper modelMapper;
    private final ReviewValidationService reviewValidationService;

    @Override
    @Transactional
    public void createReview(ReviewRequest request) {
        // Lấy thông tin người dùng hiện tại đang đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ THÊM validation này:
        reviewValidationService.validateReviewEligibility(currentUser.getId(), request.getHotelId());

        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel not found with ID: " + request.getHotelId()));

        // Rest of existing code giữ nguyên...
        Review review = new Review();
        review.setUser(currentUser);
        review.setHotel(hotel);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCleanlinessRating(request.getCleanlinessRating());
        review.setServiceRating(request.getServiceRating());
        review.setComfortRating(request.getComfortRating());
        review.setLocationRating(request.getLocationRating());
        review.setValueRating(request.getValueRating());
        review.setReviewDate(LocalDateTime.now());
        review.setIsApproved(false);

        review = reviewRepository.save(review);

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String imageUrl : request.getImageUrls()) {
                ReviewImage reviewImage = new ReviewImage();
                reviewImage.setReview(review);
                reviewImage.setImageUrl(imageUrl);
                reviewImageRepository.save(reviewImage);
            }
        }
    }

    @Override
    public boolean canUserReviewHotel(Long hotelId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return reviewValidationService.canReviewHotel(currentUser.getId(), hotelId);
    }

    @Override
    public List<Long> getHotelsEligibleForReview() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return reviewValidationService.getHotelsEligibleForReview(currentUser.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER') and @reviewSecurity.isReviewOwner(#id)")
    public void updateReview(Long id, ReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + id));
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCleanlinessRating(request.getCleanlinessRating());
        review.setServiceRating(request.getServiceRating());
        review.setComfortRating(request.getComfortRating());
        review.setLocationRating(request.getLocationRating());
        review.setValueRating(request.getValueRating());
        review.setReviewDate(LocalDateTime.now());
        review.setIsApproved(false); // Khi cập nhật, cần phê duyệt lại

        // Xử lý hình ảnh - xóa hình ảnh cũ
        if (review.getImages() != null) {
            reviewImageRepository.deleteAll(review.getImages());
            review.getImages().clear();
        }

        // Thêm hình ảnh mới
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String imageUrl : request.getImageUrls()) {
                ReviewImage reviewImage = new ReviewImage();
                reviewImage.setReview(review);
                reviewImage.setImageUrl(imageUrl);
                reviewImageRepository.save(reviewImage);
            }
        }

        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + id));
        reviewRepository.delete(review);
    }

    @Override
    @Transactional
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + id));
        review.setIsApproved(true);
        reviewRepository.save(review);
    }

    @Override
    public ReviewResponse getReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + id));
        return convertToResponse(review);
    }

    @Override
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> getReviewsByHotelId(Long hotelId) {
        return reviewRepository.findByHotelId(hotelId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> getApprovedReviewsByHotelId(Long hotelId) {
        return reviewRepository.findByHotelIdAndIsApprovedTrue(hotelId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse convertToResponse(Review review) {
        ReviewResponse response = modelMapper.map(review, ReviewResponse.class);

        // Map thông tin người dùng
        if (review.getUser() != null) {
            response.setUserId(review.getUser().getId());
            response.setUsername(review.getUser().getUsername());
            response.setUserFullName(review.getUser().getFullName());
            response.setUserProfileImage(review.getUser().getProfileImage());
        }

        // Map thông tin khách sạn
        if (review.getHotel() != null) {
            response.setHotelId(review.getHotel().getId());
            response.setHotelName(review.getHotel().getHotelName());
        }

        // Map danh sách hình ảnh
        List<String> imageUrls = new ArrayList<>();
        if (review.getImages() != null) {
            imageUrls = review.getImages().stream()
                    .map(ReviewImage::getImageUrl)
                    .collect(Collectors.toList());
        }
        response.setImageUrls(imageUrls);

        return response;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('HOTEL_MANAGER')")
    public List<ReviewResponse> getPendingReviews() {
        return reviewRepository.findByIsApprovedFalse().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getHotelReviewStats(Long hotelId) {
        List<Review> reviews = reviewRepository.findByHotelIdAndIsApprovedTrue(hotelId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReviews", reviews.size());

        if (!reviews.isEmpty()) {
            double avgRating = reviews.stream()
                    .mapToDouble(r -> r.getRating().doubleValue())
                    .average()
                    .orElse(0.0);

            double avgCleanlinessRating = reviews.stream()
                    .filter(r -> r.getCleanlinessRating() != null)
                    .mapToDouble(r -> r.getCleanlinessRating().doubleValue())
                    .average()
                    .orElse(0.0);

            double avgServiceRating = reviews.stream()
                    .filter(r -> r.getServiceRating() != null)
                    .mapToDouble(r -> r.getServiceRating().doubleValue())
                    .average()
                    .orElse(0.0);

            double avgComfortRating = reviews.stream()
                    .filter(r -> r.getComfortRating() != null)
                    .mapToDouble(r -> r.getComfortRating().doubleValue())
                    .average()
                    .orElse(0.0);

            double avgLocationRating = reviews.stream()
                    .filter(r -> r.getLocationRating() != null)
                    .mapToDouble(r -> r.getLocationRating().doubleValue())
                    .average()
                    .orElse(0.0);

            double avgValueRating = reviews.stream()
                    .filter(r -> r.getValueRating() != null)
                    .mapToDouble(r -> r.getValueRating().doubleValue())
                    .average()
                    .orElse(0.0);

            Map<Integer, Long> ratingDistribution = reviews.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getRating().intValue(),
                            Collectors.counting()
                    ));

            stats.put("averageRating", avgRating);
            stats.put("averageCleanlinessRating", avgCleanlinessRating);
            stats.put("averageServiceRating", avgServiceRating);
            stats.put("averageComfortRating", avgComfortRating);
            stats.put("averageLocationRating", avgLocationRating);
            stats.put("averageValueRating", avgValueRating);
            stats.put("ratingDistribution", ratingDistribution);
        }

        return stats;
    }
}