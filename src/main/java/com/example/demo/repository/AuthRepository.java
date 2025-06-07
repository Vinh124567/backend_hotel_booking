package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    @Query("SELECT a FROM User a JOIN FETCH a.roles WHERE a.id = :id")
    User findAdminWithRoles(@Param("id") Long id);
    Optional<User> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);
}
