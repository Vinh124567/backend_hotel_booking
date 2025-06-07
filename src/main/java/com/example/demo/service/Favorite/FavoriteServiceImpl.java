package com.example.demo.service.Favorite;

import com.example.demo.dto.hotel.HotelResponse;
import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.FavoriteRepository;
import com.example.demo.repository.HotelRepository;
import com.example.demo.service.user.UserService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final HotelRepository hotelRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;


    // Lấy User hiện tại đang đăng nhập
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập");
        }

        String username = authentication.getName();
        return userService.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy thông tin người dùng"));
    }

    @Override
    @Transactional
    public void addFavorite(Long hotelId) {
        User currentUser = getCurrentUser();
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + hotelId));

        // Kiểm tra xem đã có trong danh sách yêu thích chưa
        if (!favoriteRepository.existsByUserIdAndHotelId(currentUser.getId(), hotelId)) {
            FavoriteId favoriteId = new FavoriteId();
            favoriteId.setUserId(currentUser.getId());
            favoriteId.setHotelId(hotelId);

            Favorite favorite = new Favorite();
            favorite.setId(favoriteId);
            favorite.setUser(currentUser);
            favorite.setHotel(hotel);
            favorite.setAddedDate(LocalDateTime.now());

            favoriteRepository.save(favorite);
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long hotelId) {
        User currentUser = getCurrentUser();
        favoriteRepository.deleteByIdUserIdAndIdHotelId(currentUser.getId(), hotelId);
    }

    @Override
    public boolean isFavorite(Long hotelId) {
        User currentUser = getCurrentUser();
        return favoriteRepository.existsByUserIdAndHotelId(currentUser.getId(), hotelId);
    }

    @Override
    public List<HotelResponse> getFavoriteHotels() {
        User currentUser = getCurrentUser();
        List<Hotel> favoriteHotels = favoriteRepository.findHotelsByUserId(currentUser.getId());

        // Chuyển đổi từng khách sạn sang HotelResponse với thông tin cơ bản
        return favoriteHotels.stream()
                .map(this::convertToBasicResponse)
                .collect(Collectors.toList());
    }

    // Sử dụng lại phương thức convertToBasicResponse từ HotelService
    private HotelResponse convertToBasicResponse(Hotel hotel) {
        HotelResponse response = new HotelResponse();

        // Map thông tin cơ bản
        response.setId(hotel.getId());
        response.setHotelName(hotel.getHotelName());
        response.setAddress(hotel.getAddress());
        response.setStarRating(hotel.getStarRating());
        response.setPhoneNumber(hotel.getPhoneNumber());
        response.setPropertyType(hotel.getPropertyType());
        response.setDistanceToBeach(hotel.getDistanceToBeach());
        response.setHotelCategory(hotel.getHotelCategory());

        // Không map location
        response.setLocation(null);

        // Chỉ lấy hình ảnh chính hoặc hình đầu tiên
        if (hotel.getImages() != null && !hotel.getImages().isEmpty()) {
            List<HotelImageResponse> imageResponses = new ArrayList<>();

            // Tìm ảnh chính hoặc lấy ảnh đầu tiên
            HotelImage primaryImage = hotel.getImages().stream()
                    .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                    .findFirst()
                    .orElse(hotel.getImages().iterator().next());

            imageResponses.add(modelMapper.map(primaryImage, HotelImageResponse.class));
            response.setImages(imageResponses);
        }

        // Tính điểm đánh giá trung bình và số lượng đánh giá
        if (hotel.getReviews() != null && !hotel.getReviews().isEmpty()) {
            Double avgRating = hotel.getReviews().stream()
                    .mapToDouble(review -> review.getRating().doubleValue())
                    .average()
                    .orElse(0.0);
            response.setAverageRating(avgRating);
            response.setReviewCount(hotel.getReviews().size());
        } else {
            response.setAverageRating(0.0);
            response.setReviewCount(0);
        }

        // Không map các trường không cần thiết
        response.setAmenities(null);
        response.setDescription(null);
        response.setEmail(null);
        response.setWebsite(null);
        response.setRoomTypes(null);

        return response;
    }
}