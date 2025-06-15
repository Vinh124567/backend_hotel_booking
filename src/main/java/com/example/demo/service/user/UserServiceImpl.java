package com.example.demo.service.user;

import com.example.demo.dto.user.UserRequestDto;
import com.example.demo.dto.user.UserUpdateDto;
import com.example.demo.dto.user.UserResponseDto;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.utils.ImageUtils;

import lombok.AllArgsConstructor;

import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public void createUser(UserRequestDto requestDto) {
        User user = modelMapper.map(requestDto, User.class);
        List<Role> roles = Optional.ofNullable(requestDto.getRoleIds())
                .filter(ids -> !ids.isEmpty())
                .map(roleRepository::findAllById)
                .orElse(Collections.emptyList());
        user.setRoles(roles);
        userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Không tìm thấy thông tin người dùng"));
    }

    @Override
    public UserResponseDto getCurrentUserProfile() {
        User currentUser = getCurrentUser();
        return modelMapper.map(currentUser, UserResponseDto.class);
    }

    @Override
    @Transactional
    public UserResponseDto updateCurrentUser(UserUpdateDto updateDto) {
        User currentUser = getCurrentUser();

        // Cập nhật các field được phép
        if (updateDto.getFullName() != null && !updateDto.getFullName().trim().isEmpty()) {
            currentUser.setFullName(updateDto.getFullName().trim());
        }

        if (updateDto.getPhoneNumber() != null && !updateDto.getPhoneNumber().trim().isEmpty()) {
            currentUser.setPhoneNumber(updateDto.getPhoneNumber().trim());
        }

        if (updateDto.getAddress() != null && !updateDto.getAddress().trim().isEmpty()) {
            currentUser.setAddress(updateDto.getAddress().trim());
        }

        if (updateDto.getDateOfBirth() != null) {
            currentUser.setDateOfBirth(updateDto.getDateOfBirth());
        }

        if (updateDto.getGender() != null && !updateDto.getGender().trim().isEmpty()) {
            currentUser.setGender(updateDto.getGender().trim());
        }

        // Xử lý profile image - pattern giống RoomImage
        if (updateDto.getProfileImage() != null) {
            if (updateDto.getProfileImage().isEmpty()) {
                // Xóa ảnh hiện tại
                currentUser.setProfileImage(null);
            } else {
                // Lưu ảnh mới - sử dụng method giống RoomImage
                String imageUrl = saveBase64Image(updateDto.getProfileImage());
                currentUser.setProfileImage(imageUrl);
            }
        }

        User updatedUser = userRepository.save(currentUser);
        return modelMapper.map(updatedUser, UserResponseDto.class);
    }

    // ================ PRIVATE HELPER METHOD ================

    /**
     * Lưu base64 image - pattern giống RoomImage service
     */
    private String saveBase64Image(String base64Image) {
        try {
            return ImageUtils.saveBase64Image(base64Image);
        } catch (Exception e) {
            throw new RuntimeException("Không thể lưu ảnh đại diện");
        }
    }
}