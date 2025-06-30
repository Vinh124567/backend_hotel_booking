package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_type_id")
    @ToString.Exclude
    private RoomType roomType;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "special_requests")
    private String specialRequests;

    @Column(name = "status")
    private String status = "Chờ xác nhận";  // Enum: 'Chờ xác nhận', 'Đã xác nhận', 'Đã hủy', 'Hoàn thành'

    // ✅ THÊM 2 FIELDS MỚI CHO DEPOSIT PAYMENT
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "remaining_amount", precision = 10, scale = 2)
    private BigDecimal remainingAmount;

    @ManyToOne
    @JoinColumn(name = "cancellation_policy_id")
    @ToString.Exclude
    private CancellationPolicy cancellationPolicy;

    @ManyToOne
    @JoinColumn(name = "assigned_room_id")
    @ToString.Exclude
    private Room assignedRoom;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<GuestInformation> guestInformation = new HashSet<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<Payment> payments = new HashSet<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Review review;

    @OneToMany(mappedBy = "relatedBooking", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @ToString.Exclude
    private Set<PromotionUsage> promotionUsages = new HashSet<>();
    // ✅ THÊM: Deposit percentage field
    @Column(name = "deposit_percentage", precision = 5, scale = 2)
    private BigDecimal depositPercentage;


    @PrePersist
    protected void onCreate() {
        this.bookingDate = LocalDateTime.now();
    }

    // ✅ HELPER METHODS CHO DEPOSIT PAYMENT (optional)
    public boolean hasDeposit() {
        return depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasRemainingAmount() {
        return remainingAmount != null && remainingAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isFullyPaid() {
        return !hasRemainingAmount() || remainingAmount.equals(BigDecimal.ZERO);
    }
}