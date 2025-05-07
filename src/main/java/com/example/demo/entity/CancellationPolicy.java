package com.example.demo.entity;

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
@Table(name = "cancellation_policies")
public class CancellationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "free_cancellation_hours")
    private Integer freeCancellationHours;

    @Column(name = "cancellation_fee_percentage", precision = 5, scale = 2)
    private BigDecimal cancellationFeePercentage;

    @OneToMany(mappedBy = "cancellationPolicy")
    private Set<Booking> bookings = new HashSet<>();
}
