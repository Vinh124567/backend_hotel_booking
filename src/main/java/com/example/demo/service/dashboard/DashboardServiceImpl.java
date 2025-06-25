package com.example.demo.service.dashboard;

import com.example.demo.dto.dashboard.DashboardOverviewDto;
import com.example.demo.dto.dashboard.RecentBookingDto;
import com.example.demo.dto.hotel.TopPerformingHotelDto;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public DashboardOverviewDto getDashboardOverview() {
        // Đếm tổng số khách sạn
        Long totalHotels = hotelRepository.count();

        // Đếm tổng số người dùng
        Long totalUsers = userRepository.count();

        // Đếm booking hôm nay
        LocalDate today = LocalDate.now();
        Long todayBookings = bookingRepository.countByBookingDateBetween(
                today.atStartOfDay(),
                today.atTime(23, 59, 59)
        );

        // Tính doanh thu hôm nay
        BigDecimal todayRevenue = paymentRepository.getTodayRevenue(today)
                .orElse(BigDecimal.ZERO);

        // Đếm booking pending
        Long pendingBookings = bookingRepository.countByStatus("Chờ xác nhận");

        // Đếm payment pending
        Long pendingPayments = paymentRepository.countByPaymentStatus("Chờ thanh toán");

        return DashboardOverviewDto.builder()
                .totalHotels(totalHotels)
                .totalUsers(totalUsers)
                .todayBookings(todayBookings)
                .todayRevenue(todayRevenue)
                .pendingBookings(pendingBookings)
                .pendingPayments(pendingPayments)
                .build();
    }

    @Override
    public List<RecentBookingDto> getRecentBookings() {
        var bookings = bookingRepository.findRecentBookingsWithDetails()
                .stream()
                .limit(10) // Giới hạn 10 records trong Java
                .collect(Collectors.toList());

        return bookings.stream()
                .map(booking -> RecentBookingDto.builder()
                        .bookingId(booking.getId())
                        .userName(booking.getUser() != null ? booking.getUser().getFullName() : "N/A")
                        .hotelName(booking.getRoomType() != null && booking.getRoomType().getHotel() != null
                                ? booking.getRoomType().getHotel().getHotelName() : "N/A")
                        .checkInDate(booking.getCheckInDate())
                        .checkOutDate(booking.getCheckOutDate())
                        .totalPrice(booking.getTotalPrice())
                        .status(booking.getStatus())
                        .bookingDate(booking.getBookingDate())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TopPerformingHotelDto> getTopHotels() {
        List<Object[]> results = (List<Object[]>) hotelRepository.findTopHotelsByRevenue();

        return results.stream()
                .map(this::mapToTopPerformingHotelDto)
                .collect(Collectors.toList());
    }

    // ✅ Method riêng để mapping
    private TopPerformingHotelDto mapToTopPerformingHotelDto(Object[] result) {
        return TopPerformingHotelDto.builder()
                .hotelId(((Number) result[0]).longValue())
                .hotelName((String) result[1])
                .location((String) result[2])
                .totalBookings(((Number) result[4]).longValue())
                .monthlyRevenue(new BigDecimal(result[5].toString()))
                .averageRating(((Number) result[6]).doubleValue())
                .status("⭐ " + result[3] + " sao")
                .occupancyRate(calculateOccupancyRate(((Number) result[0]).longValue()))
                .build();
    }

    private Double calculateOccupancyRate(Long hotelId) {
        return Math.random() * 100;
    }
}