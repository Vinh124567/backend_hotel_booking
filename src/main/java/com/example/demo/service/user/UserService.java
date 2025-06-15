package com.example.demo.service.user;

import com.example.demo.dto.user.UserRequestDto;
import com.example.demo.dto.user.UserUpdateDto;
import com.example.demo.dto.user.UserResponseDto;
import com.example.demo.entity.User;

import java.util.Optional;

public interface UserService {
    /**
     * Tạo người dùng mới
     */
    void createUser(UserRequestDto requestDto);

    /**
     * Tìm người dùng theo username
     * @param username Tên đăng nhập
     * @return Optional chứa User nếu tìm thấy, ngược lại là Optional.empty()
     */
    Optional<User> findByUsername(String username);

    /**
     * Lấy thông tin người dùng hiện tại đang đăng nhập
     * @return User hiện tại
     */
    User getCurrentUser();

    /**
     * Lấy thông tin profile người dùng hiện tại
     * @return UserResponseDto
     */
    UserResponseDto getCurrentUserProfile();

    /**
     * Cập nhật thông tin người dùng hiện tại
     * @param updateDto Thông tin cần cập nhật
     * @return UserResponseDto đã được cập nhật
     */
    UserResponseDto updateCurrentUser(UserUpdateDto updateDto);
}
