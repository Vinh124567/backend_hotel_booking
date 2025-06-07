package com.example.demo.service.hotel;

import com.example.demo.dto.amenity.AmenityResponse;
import com.example.demo.dto.hotel.HotelRequest;
import com.example.demo.dto.hotel.HotelResponse;
import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.dto.room_type.RoomTypeResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.utils.ImageUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.demo.utils.ImageUtils.saveBase64Image;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {
    private final HotelRepository hotelRepository;
    private final LocationRepository locationRepository;
    private final AmenityRepository amenityRepository;
    private final HotelImageRepository hotelImageRepository;
    private final ModelMapper modelMapper;

    @Override
    public Hotel createHotel(HotelRequest request) {
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy location với ID: " + request.getLocationId()));

        Hotel hotel = modelMapper.map(request, Hotel.class);
        hotel.setLocation(location);

        // Thiết lập images thành tập rỗng trước khi lưu
        hotel.setImages(new HashSet<>());

        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            hotel.setAmenities(amenities);
        }

        Hotel savedHotel = hotelRepository.save(hotel);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            Set<HotelImage> hotelImages = request.getImages().stream()
                    .map(imageRequest -> {
                        HotelImage image = new HotelImage();

                        // Log chuỗi base64 (giới hạn 100 ký tự đầu)
                        String rawBase64 = imageRequest.getImageUrl();
                        System.out.println("Raw base64 input (first 100 chars): " +
                                (rawBase64.length() > 100 ? rawBase64.substring(0, 100) + "..." : rawBase64));

                        // Gọi hàm xử lý base64
                        String imageUrl = ImageUtils.saveBase64Image(rawBase64);

                        // Log kết quả sau khi xử lý
                        System.out.println("Generated image URL: " + imageUrl);

                        image.setImageUrl(imageUrl);
                        image.setCaption(imageRequest.getCaption());
                        image.setIsPrimary(Boolean.TRUE.equals(imageRequest.getIsPrimary()));
                        image.setHotel(savedHotel);
                        return image;
                    })
                    .collect(Collectors.toSet());

            hotelImageRepository.saveAll(hotelImages);
            savedHotel.setImages(hotelImages);
        }

        return savedHotel;
    }

    @Override
    public void deleteHotel(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + id));
        hotelRepository.delete(hotel);
    }

    @Override
    public Hotel updateHotel(Long id, HotelRequest request) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + id));

        // Cập nhật các trường thông tin cơ bản
        modelMapper.map(request, hotel);

        // Cập nhật location
        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy location với ID: " + request.getLocationId()));
        hotel.setLocation(location);

        // Cập nhật amenities
        if (request.getAmenityIds() != null) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            hotel.setAmenities(amenities);
        }

        // Cập nhật hình ảnh (xóa cũ - thêm mới với xử lý base64)
        if (request.getImages() != null) {
            // Xóa ảnh cũ
            hotelImageRepository.deleteAll(hotel.getImages());

            // Lưu ảnh mới từ base64
            Set<HotelImage> hotelImages = request.getImages().stream()
                    .map(imageRequest -> {
                        HotelImage image = new HotelImage();

                        // Lưu ảnh và lấy URL
                        String imageUrl = saveBase64Image(imageRequest.getImageUrl());
                        image.setImageUrl(imageUrl);

                        image.setHotel(hotel);
                        return image;
                    })
                    .collect(Collectors.toSet());

            hotelImageRepository.saveAll(hotelImages);
            hotel.setImages(hotelImages);
        }

        return hotelRepository.save(hotel);
    }

    @Override
    public HotelResponse getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + id));

        // Khởi tạo đối tượng HotelResponse
        HotelResponse response = modelMapper.map(hotel, HotelResponse.class);

        // Map danh sách hình ảnh
        if (hotel.getImages() != null && !hotel.getImages().isEmpty()) {
            List<HotelImageResponse> imageResponses = hotel.getImages().stream()
                    .map(image -> modelMapper.map(image, HotelImageResponse.class))
                    .collect(Collectors.toList());
            response.setImages(imageResponses);
        }

        // Map danh sách tiện nghi
        if (hotel.getAmenities() != null && !hotel.getAmenities().isEmpty()) {
            List<AmenityResponse> amenityResponses = hotel.getAmenities().stream()
                    .map(amenity -> modelMapper.map(amenity, AmenityResponse.class))
                    .collect(Collectors.toList());
            response.setAmenities(amenityResponses);
        }

        // THÊM DÒNG NÀY: Map danh sách loại phòng
        if (hotel.getRoomTypes() != null && !hotel.getRoomTypes().isEmpty()) {
            List<RoomTypeResponse> roomTypeResponses = hotel.getRoomTypes().stream()
                    .map(roomType -> modelMapper.map(roomType, RoomTypeResponse.class))
                    .collect(Collectors.toList());
            response.setRoomTypes(roomTypeResponses);
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

        return response;
    }

    @Override
    public List<HotelResponse> getAllHotelsBasic() {
        List<Hotel> hotels = hotelRepository.findAll();
        return hotels.stream()
                .map(this::convertToBasicResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<HotelResponse> filterHotels(String cityName, Double minPrice, Double maxPrice, Double minRating, List<Long> amenityIds, Integer numberOfGuests) {
        return hotelRepository.findAll().stream()
                .filter(hotel -> hotel.getIsActive() != null && hotel.getIsActive())
                .filter(hotel -> cityName == null || (hotel.getLocation() != null &&
                        hotel.getLocation().getCityName() != null &&
                        hotel.getLocation().getCityName().toLowerCase().contains(cityName.toLowerCase())))
                .filter(hotel -> minPrice == null || (hotel.getRoomTypes() != null &&
                        hotel.getRoomTypes().stream().anyMatch(rt -> rt.getBasePrice() != null &&
                                rt.getBasePrice().doubleValue() >= minPrice)))
                .filter(hotel -> maxPrice == null || (hotel.getRoomTypes() != null &&
                        hotel.getRoomTypes().stream().anyMatch(rt -> rt.getBasePrice() != null &&
                                rt.getBasePrice().doubleValue() <= maxPrice)))
                .filter(hotel -> minRating == null || (hotel.getReviews() != null && !hotel.getReviews().isEmpty() &&
                        hotel.getReviews().stream().mapToDouble(r -> r.getRating().doubleValue()).average().orElse(0.0) >= minRating))
                .filter(hotel -> amenityIds == null || amenityIds.isEmpty() || (hotel.getAmenities() != null &&
                        hotel.getAmenities().stream().anyMatch(a -> amenityIds.contains(a.getId()))))
                .filter(hotel -> numberOfGuests == null || (hotel.getRoomTypes() != null &&
                        hotel.getRoomTypes().stream().anyMatch(rt -> rt.getMaxOccupancy() != null &&
                                rt.getMaxOccupancy() >= numberOfGuests)))
                .map(this::convertToBasicResponse)
                .collect(Collectors.toList());
    }

    // Thêm phương thức private để tái sử dụng code
    private HotelResponse convertToBasicResponse(Hotel hotel) {
        HotelResponse response = new HotelResponse();

        // Map thông tin cơ bản
        response.setId(hotel.getId());
        response.setHotelName(hotel.getHotelName());
        response.setAddress(hotel.getAddress());
        response.setStarRating(hotel.getStarRating());
        response.setPhoneNumber(hotel.getPhoneNumber());
        response.setPropertyType(hotel.getPropertyType());


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

        return response;
    }
}
