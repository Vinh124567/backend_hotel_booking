package com.example.demo.service.user;

import com.example.demo.dto.user.UserRequestDto;
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
}