package com.example.demo.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userFullName;
    private String userProfileImage;
    private Long hotelId;
    private String hotelName;
    private BigDecimal rating;
    private String comment;
    private BigDecimal cleanlinessRating;
    private BigDecimal serviceRating;
    private BigDecimal comfortRating;
    private BigDecimal locationRating;
    private BigDecimal valueRating;
    private LocalDateTime reviewDate;
    private Boolean isApproved;
    private List<String> imageUrls;
}