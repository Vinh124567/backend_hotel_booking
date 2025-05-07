package com.example.demo.service.location;

import com.example.demo.dto.location.LocationRequest;
import com.example.demo.entity.Location;
import com.example.demo.entity.Room;
import com.example.demo.repository.LocationRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@AllArgsConstructor
@Service
public class LocationServiceImpl implements LocationService {

    @Autowired
    private LocationRepository locationRepository;
    private final ModelMapper modelMapper;

    @Override
    public Location createLocation(LocationRequest request) {
        Location location = modelMapper.map(request, Location.class);
        return locationRepository.save(location);
    }

    @Override
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    @Override
    public Location getLocationById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
    }
}

