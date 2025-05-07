package com.example.demo.dto.user;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class UserRequestDto {
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
    private String profileImage;
    private List<Long> roleIds;
}
