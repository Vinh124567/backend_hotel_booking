package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm user theo username
    Optional<User> findByUsername(String username);

    // Tìm user theo email
    Optional<User> findByEmail(String email);

    // Kiểm tra username đã tồn tại chưa
    boolean existsByUsername(String username);

    // Kiểm tra email đã tồn tại chưa
    boolean existsByEmail(String email);

    // Tìm user theo reset token
    Optional<User> findByResetToken(String resetToken);

    // Tìm users có token chưa hết hạn
    List<User> findByResetTokenIsNotNullAndResetTokenExpiryAfter(LocalDateTime now);

    // Tìm user theo số điện thoại
    Optional<User> findByPhoneNumber(String phoneNumber);

    // Tìm users theo trạng thái active
    List<User> findByIsActive(Boolean isActive);

    // Tìm users đăng ký mới trong khoảng thời gian
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Đếm số lượng người dùng theo vai trò
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    Long countByRoleName(@Param("roleName") String roleName);

    // Tìm users chưa đăng nhập trong khoảng thời gian
    List<User> findByLastLoginBeforeOrLastLoginIsNull(LocalDateTime cutoffDate);

    // Lấy danh sách người dùng thường có nhiều đánh giá nhất
    @Query("SELECT u FROM User u JOIN u.reviews r GROUP BY u.id ORDER BY COUNT(r) DESC")
    List<User> findTopReviewers(org.springframework.data.domain.Pageable pageable);

    // Lấy danh sách người dùng có nhiều đặt phòng nhất
    @Query("SELECT u FROM User u JOIN u.bookings b GROUP BY u.id ORDER BY COUNT(b) DESC")
    List<User> findTopBookers(org.springframework.data.domain.Pageable pageable);

    // Tìm kiếm user theo tên đầy đủ
    List<User> findByFullNameContainingIgnoreCase(String fullName);

    // Lấy tất cả IDs của users (corrected to use Long)
    @Query("SELECT u.id FROM User u")
    List<Long> findAllUserIds();
}