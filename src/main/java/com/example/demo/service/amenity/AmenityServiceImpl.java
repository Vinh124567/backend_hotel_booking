package com.example.demo.service.amenity;


import com.example.demo.dto.amenity.AmenityRequest;
import com.example.demo.entity.Amenity;
import com.example.demo.entity.User;
import com.example.demo.repository.AmenityRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AmenityServiceImpl implements AmenityService {
    private final AmenityRepository amenityRepository;
    private final ModelMapper modelMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void createAmenity(AmenityRequest request) {
        Amenity amenity = modelMapper.map(request, Amenity.class);
        amenityRepository.save(amenity);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void updateAmenity(Long id, AmenityRequest request) {
        Amenity existingAmenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tiện nghi không tồn tại với id: " + id));

        modelMapper.map(request, existingAmenity);
        amenityRepository.save(existingAmenity);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void deleteAmenity(Long id) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found with ID: " + id));
        amenityRepository.delete(amenity);
    }

}
