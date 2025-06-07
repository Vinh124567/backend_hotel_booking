package com.example.demo.service.amenity;

import com.example.demo.dto.amenity.AmenityRequest;
import com.example.demo.dto.amenity.AmenityResponse;
import com.example.demo.entity.Amenity;
import com.example.demo.entity.Hotel;
import com.example.demo.enumm.AmenityCategory;
import com.example.demo.repository.AmenityRepository;
import com.example.demo.repository.HotelRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AmenityServiceImpl implements AmenityService {
    private final AmenityRepository amenityRepository;
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void createAmenity(AmenityRequest request) {
        Amenity amenity = modelMapper.map(request, Amenity.class);

        // Phân loại tiện ích nếu chưa có category
        categorizeAmenity(amenity);

        amenityRepository.save(amenity);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void updateAmenity(Long id, AmenityRequest request) {
        Amenity existingAmenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tiện nghi không tồn tại với id: " + id));

        modelMapper.map(request, existingAmenity);

        // Phân loại tiện ích nếu chưa có category
        categorizeAmenity(existingAmenity);

        amenityRepository.save(existingAmenity);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void deleteAmenity(Long id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tiện nghi không tồn tại với id: " + id));
        amenityRepository.delete(amenity);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public List<AmenityResponse> getAmenitiesByHotelId(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Khách sạn không tồn tại với id: " + hotelId));

        List<AmenityResponse> amenityResponses = hotel.getAmenities().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return amenityResponses;
    }

    // Phương thức chuyển đổi Amenity -> AmenityResponse
    private AmenityResponse convertToResponse(Amenity amenity) {
        AmenityResponse response = modelMapper.map(amenity, AmenityResponse.class);

        if (amenity.getCategory() != null) {
            response.setCategoryDisplayName(amenity.getCategory().getDisplayName());
        }

        return response;
    }

    // Phương thức phân loại tiện ích tự động
    private void categorizeAmenity(Amenity amenity) {
        if (amenity.getCategory() == null) {
            String name = amenity.getAmenityName().toLowerCase();

            if (name.contains("wi-fi") || name.contains("wifi") || name.contains("internet") ||
                    name.contains("mạng") || name.contains("kết nối")) {
                amenity.setCategory(AmenityCategory.CONNECTIVITY);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("wifi");
                }
            }
            else if (name.contains("bữa sáng") || name.contains("bữa trưa") ||
                    name.contains("bữa tối") || name.contains("nhà hàng") ||
                    name.contains("đồ uống") || name.contains("bar") ||
                    name.contains("buffet") || name.contains("ăn uống") ||
                    name.contains("quán ăn") || name.contains("cà phê")) {
                amenity.setCategory(AmenityCategory.FOOD_AND_DRINK);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("restaurant");
                }
            }
            else if (name.contains("đỗ xe") || name.contains("bãi đậu xe") ||
                    name.contains("xe") || name.contains("đưa đón") ||
                    name.contains("di chuyển") || name.contains("thuê xe") ||
                    name.contains("sân bay") || name.contains("xe đạp")) {
                amenity.setCategory(AmenityCategory.TRANSPORTATION);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("directions_car");
                }
            }
            else if (name.contains("hồ bơi") || name.contains("bể bơi") ||
                    name.contains("spa") || name.contains("gym") ||
                    name.contains("thể dục") || name.contains("vườn") ||
                    name.contains("sân") || name.contains("điều hòa") ||
                    name.contains("máy lạnh") || name.contains("không khí")) {
                amenity.setCategory(AmenityCategory.GENERAL);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("spa");
                }
            }
            else if (name.contains("dịch vụ") || name.contains("giặt ủi") ||
                    name.contains("dọn phòng") || name.contains("phục vụ")) {
                amenity.setCategory(AmenityCategory.HOTEL_SERVICE);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("room_service");
                }
            }
            else if (name.contains("họp") || name.contains("hội nghị") ||
                    name.contains("doanh nhân") || name.contains("fax") ||
                    name.contains("photocopy") || name.contains("văn phòng")) {
                amenity.setCategory(AmenityCategory.BUSINESS_FACILITIES);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("business_center");
                }
            }
            else if (name.contains("biển") || name.contains("mua sắm") ||
                    name.contains("du lịch") || name.contains("gần") ||
                    name.contains("lân cận") || name.contains("chợ")) {
                amenity.setCategory(AmenityCategory.NEARBY_FACILITIES);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("location_on");
                }
            }
            else if (name.contains("trẻ em") || name.contains("trẻ con") ||
                    name.contains("em bé") || name.contains("khu vui chơi") ||
                    name.contains("sân chơi") || name.contains("giữ trẻ")) {
                amenity.setCategory(AmenityCategory.KIDS);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("child_care");
                }
            }
            else if (name.contains("thang máy") || name.contains("khu vực hút thuốc") ||
                    name.contains("sảnh") || name.contains("lưu trữ") ||
                    name.contains("hành lý") || name.contains("két an toàn")) {
                amenity.setCategory(AmenityCategory.PUBLIC_FACILITIES);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("public");
                }
            }
            else {
                amenity.setCategory(AmenityCategory.GENERAL);
                if (amenity.getIconCode() == null) {
                    amenity.setIconCode("settings");
                }
            }
        }

        // Đảm bảo có iconCode nếu category đã được thiết lập nhưng iconCode chưa
        if (amenity.getIconCode() == null && amenity.getCategory() != null) {
            switch (amenity.getCategory()) {
                case CONNECTIVITY:
                    amenity.setIconCode("wifi");
                    break;
                case FOOD_AND_DRINK:
                    amenity.setIconCode("restaurant");
                    break;
                case TRANSPORTATION:
                    amenity.setIconCode("directions_car");
                    break;
                case HOTEL_SERVICE:
                    amenity.setIconCode("room_service");
                    break;
                case BUSINESS_FACILITIES:
                    amenity.setIconCode("business_center");
                    break;
                case NEARBY_FACILITIES:
                    amenity.setIconCode("location_on");
                    break;
                case KIDS:
                    amenity.setIconCode("child_care");
                    break;
                case PUBLIC_FACILITIES:
                    amenity.setIconCode("public");
                    break;
                default:
                    amenity.setIconCode("settings");
                    break;
            }
        }
    }
}