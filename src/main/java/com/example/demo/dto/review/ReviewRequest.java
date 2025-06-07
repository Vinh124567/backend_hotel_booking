package com.example.demo.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    private Long hotelId;
    private BigDecimal rating;
    private String comment;
    private BigDecimal cleanlinessRating;
    private BigDecimal serviceRating;
    private BigDecimal comfortRating;
    private BigDecimal locationRating;
    private BigDecimal valueRating;
    private List<String> imageUrls;
}