package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "amenities")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "amenity_id")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "amenity_name", nullable = false, length = 100)
    private String amenityName;

    @Column(name = "amenity_type", nullable = false)
    private String amenityType;  // Enum: 'Khách sạn', 'Phòng'

    @Column(name = "icon_url")
    private String iconUrl;

    @ManyToMany(mappedBy = "amenities")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Hotel> hotels = new HashSet<>();

    @ManyToMany(mappedBy = "amenities")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<RoomType> roomTypes = new HashSet<>();
}
