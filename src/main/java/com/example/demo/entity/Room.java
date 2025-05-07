package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_type_id")
    @JsonBackReference("roomtype-room") // Giữ nguyên
    private RoomType roomType;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "floor", length = 10)
    private String floor;

    @Column(name = "status")
    private String status = "Trống";

    @OneToMany(mappedBy = "assignedRoom")
    @JsonIgnore // Thay thế JsonManagedReference để tránh vòng lặp
    private Set<Booking> bookings = new HashSet<>();
}
