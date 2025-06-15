package com.example.demo.controller;

import com.example.demo.dto.user.UserRequestDto;
import com.example.demo.dto.user.UserUpdateDto;
import com.example.demo.dto.user.UserResponseDto;
import com.example.demo.response.ApiResponse;
import com.example.demo.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // Endpoint hiện có - tạo user
    @PostMapping()
    public ResponseEntity<?> createUser(@RequestBody UserRequestDto user) {
        String encodedPassword = passwordEncoder.encode(user.getPasswordHash());
        user.setPasswordHash(encodedPassword);
        userService.createUser(user);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setMessage("Tạo người dùng thành công");
        apiResponse.setCode(HttpStatus.OK.value());
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }

    // ===== CÁC ENDPOINT MỚI THÊM VÀO =====

    /**
     * Lấy thông tin profile người dùng hiện tại
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUserProfile() {
        UserResponseDto userProfile = userService.getCurrentUserProfile();
        ApiResponse<UserResponseDto> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userProfile);
        apiResponse.setMessage("Lấy thông tin profile thành công");
        apiResponse.setCode(HttpStatus.OK.value());
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Cập nhật thông tin profile người dùng hiện tại
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateCurrentUserProfile(
            @Valid @RequestBody UserUpdateDto updateDto) {
        UserResponseDto updatedUser = userService.updateCurrentUser(updateDto);
        ApiResponse<UserResponseDto> apiResponse = new ApiResponse<>();
        apiResponse.setResult(updatedUser);
        apiResponse.setMessage("Cập nhật thông tin thành công");
        apiResponse.setCode(HttpStatus.OK.value());
        return ResponseEntity.ok(apiResponse);
    }
}