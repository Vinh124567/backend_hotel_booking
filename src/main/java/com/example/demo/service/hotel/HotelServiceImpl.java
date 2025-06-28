package com.example.demo.service.hotel;

import com.example.demo.dto.amenity.AmenityResponse;
import com.example.demo.dto.hotel.HotelRequest;
import com.example.demo.dto.hotel.HotelResponse;
import com.example.demo.dto.hotel_image.HotelImageRequest;
import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.dto.room_type.RoomTypeResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.utils.ImageUtils;
import jakarta.transaction.Transactional;
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
    @Transactional
    public Hotel updateHotel(Long id, HotelRequest request) {
        System.out.println("🔍 === UPDATE HOTEL START ===");
        System.out.println("🔍 Hotel ID: " + id);

        try {
            // ✅ Tìm hotel
            Hotel hotel = hotelRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách sạn với ID: " + id));

            System.out.println("🔍 Found hotel: " + hotel.getHotelName());

            // ✅ Cập nhật các trường thông tin cơ bản
            modelMapper.map(request, hotel);

            // ✅ Cập nhật location
            if (request.getLocationId() != null) {
                Location location = locationRepository.findById(request.getLocationId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy location với ID: " + request.getLocationId()));
                hotel.setLocation(location);
                System.out.println("🔍 Updated location: " + location.getCityName());
            }

            // ✅ Cập nhật amenities
            if (request.getAmenityIds() != null) {
                Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
                hotel.setAmenities(amenities);
                System.out.println("🔍 Updated amenities count: " + amenities.size());
            }

            // ✅ Cập nhật hình ảnh - LOGIC CHÍNH XÁC
            if (request.getImages() != null) {
                System.out.println("🔍 === UPDATING IMAGES ===");
                System.out.println("🔍 Number of images in request: " + request.getImages().size());

                // ✅ Xóa tất cả ảnh cũ
                if (hotel.getImages() != null && !hotel.getImages().isEmpty()) {
                    System.out.println("🔍 Deleting " + hotel.getImages().size() + " old images");
                    hotelImageRepository.deleteAll(hotel.getImages());
                    hotel.getImages().clear();
                }

                // ✅ Xử lý từng ảnh mới
                Set<HotelImage> newHotelImages = new HashSet<>();

                for (int i = 0; i < request.getImages().size(); i++) {
                    HotelImageRequest imageRequest = request.getImages().get(i);

                    System.out.println("🔍 --- Processing image " + (i + 1) + " ---");
                    System.out.println("🔍 Image URL length: " + imageRequest.getImageUrl().length());
                    System.out.println("🔍 Is base64: " + ImageUtils.isBase64(imageRequest.getImageUrl()));
                    System.out.println("🔍 Is URL: " + ImageUtils.isUrl(imageRequest.getImageUrl()));

                    try {
                        HotelImage hotelImage = new HotelImage();
                        String finalImageUrl;

                        // ✅ Phân biệt base64 và URL hiện có
                        if (ImageUtils.isBase64(imageRequest.getImageUrl())) {
                            // Đây là ảnh mới (base64) -> cần convert thành file
                            System.out.println("🔍 Converting base64 to file...");
                            finalImageUrl = ImageUtils.saveBase64Image(imageRequest.getImageUrl());
                            System.out.println("✅ Converted to URL: " + finalImageUrl);

                        } else if (ImageUtils.isUrl(imageRequest.getImageUrl())) {
                            // Đây là URL hiện có -> giữ nguyên
                            finalImageUrl = imageRequest.getImageUrl();
                            System.out.println("✅ Keeping existing URL: " + finalImageUrl);

                        } else {
                            // Trường hợp không xác định
                            System.err.println("❌ Unknown image format: " + imageRequest.getImageUrl().substring(0, Math.min(50, imageRequest.getImageUrl().length())));
                            throw new RuntimeException("Định dạng ảnh không hợp lệ");
                        }

                        // ✅ Validate URL cuối cùng
                        if (finalImageUrl == null || finalImageUrl.trim().isEmpty()) {
                            throw new RuntimeException("URL ảnh không hợp lệ sau xử lý");
                        }

                        if (finalImageUrl.length() > 1000) { // Giới hạn an toàn
                            throw new RuntimeException("URL ảnh quá dài: " + finalImageUrl.length() + " ký tự");
                        }

                        // ✅ Đảm bảo URL không phải base64
                        if (ImageUtils.isBase64(finalImageUrl)) {
                            throw new RuntimeException("❌ CRITICAL: Đang cố lưu base64 vào database!");
                        }

                        // ✅ Set các thuộc tính
                        hotelImage.setImageUrl(finalImageUrl);
                        hotelImage.setHotel(hotel);
                        hotelImage.setCaption(imageRequest.getCaption() != null ? imageRequest.getCaption() : "");
                        hotelImage.setIsPrimary(imageRequest.getIsPrimary() != null ? imageRequest.getIsPrimary() : false);

                        newHotelImages.add(hotelImage);

                        System.out.println("✅ Image " + (i + 1) + " processed successfully");

                    } catch (Exception e) {
                        System.err.println("❌ Error processing image " + (i + 1) + ": " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("Lỗi xử lý ảnh " + (i + 1) + ": " + e.getMessage());
                    }
                }

                // ✅ Lưu tất cả ảnh mới vào database
                if (!newHotelImages.isEmpty()) {
                    System.out.println("🔍 Saving " + newHotelImages.size() + " images to database...");

                    try {
                        Set<HotelImage> savedImages = new HashSet<>(hotelImageRepository.saveAll(newHotelImages));
                        hotel.setImages(savedImages);
                        System.out.println("✅ All images saved to database successfully");

                        // Debug: In ra thông tin ảnh đã lưu
                        for (HotelImage img : savedImages) {
                            System.out.println("🔍 Saved image ID: " + img.getId() + ", URL length: " + img.getImageUrl().length());
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Database error saving images: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException("Lỗi lưu ảnh vào database: " + e.getMessage());
                    }
                } else {
                    System.out.println("🔍 No images to save");
                }

                System.out.println("🔍 === IMAGES UPDATE COMPLETED ===");
            }

            // ✅ Lưu hotel
            System.out.println("🔍 Saving hotel to database...");
            Hotel savedHotel = hotelRepository.save(hotel);
            System.out.println("✅ Hotel saved successfully");

            System.out.println("🔍 === UPDATE HOTEL COMPLETED ===");
            return savedHotel;

        } catch (Exception e) {
            System.err.println("❌ Error in updateHotel: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi cập nhật khách sạn: " + e.getMessage());
        }
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
            List<HotelImageResponse> imageResponses = hotel.getImages().stream()
                    .map(image -> modelMapper.map(image, HotelImageResponse.class))
                    .collect(Collectors.toList());
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
