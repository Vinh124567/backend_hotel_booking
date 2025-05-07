package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "room_types")
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_type_id")
    private Long id;

    @ManyToOne
    @JsonBackReference("hotel-roomtype") // Đặt tên để khớp với hotel
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    @Column(name = "description")
    private String description;

    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "size_sqm", precision = 6, scale = 2)
    private BigDecimal sizeSqm;

    @Column(name = "bed_type", length = 100)
    private String bedType;

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL)
    @JsonIgnore // Thay thế JsonManagedReference để tránh vòng lặp
    private Set<Room> rooms = new HashSet<>();

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL)
    @JsonIgnore // Thay thế JsonManagedReference để tránh vòng lặp
    private Set<RoomImage> images = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "room_type_amenities",
            joinColumns = @JoinColumn(name = "room_type_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @JsonIgnore // Thêm để tránh vòng lặp với amenities
    private Set<Amenity> amenities = new HashSet<>();

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL)
    @JsonIgnore // Thay thế JsonManagedReference để tránh vòng lặp
    private Set<RoomPricing> pricings = new HashSet<>();

    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL)
    @JsonIgnore // Thay thế JsonManagedReference để tránh vòng lặp
    private Set<Booking> bookings = new HashSet<>();
}
