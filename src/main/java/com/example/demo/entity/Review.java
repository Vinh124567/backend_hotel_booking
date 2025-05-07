package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "comment")
    private String comment;

    @Column(name = "cleanliness_rating", precision = 2, scale = 1)
    private BigDecimal cleanlinessRating;

    @Column(name = "service_rating", precision = 2, scale = 1)
    private BigDecimal serviceRating;

    @Column(name = "comfort_rating", precision = 2, scale = 1)
    private BigDecimal comfortRating;

    @Column(name = "location_rating", precision = 2, scale = 1)
    private BigDecimal locationRating;

    @Column(name = "value_rating", precision = 2, scale = 1)
    private BigDecimal valueRating;

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Column(name = "is_approved")
    private Boolean isApproved = false;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL)
    private Set<ReviewImage> images = new HashSet<>();
}
