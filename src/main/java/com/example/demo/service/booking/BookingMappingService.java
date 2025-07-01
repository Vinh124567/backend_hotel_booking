package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingMappingService {
    private static final int CHECK_IN_GRACE_DAYS = 1;
    private final ReviewRepository reviewRepository;
    public BookingResponse mapToBookingResponse(Booking booking) {
        BookingResponse response = new BookingResponse();

        mapBasicBookingFields(response, booking);
        mapUserInfo(response, booking.getUser());
        mapRoomTypeInfo(response, booking.getRoomType());
        mapHotelInfo(response, booking.getRoomType().getHotel());
        mapPaymentInfo(response, booking.getPayments());

        // ✅ THÊM: Map deposit fields BEFORE permissions
        mapDepositInfo(response, booking);

        mapPermissions(response, booking);
        mapReviewInfo(response, booking);

        return response;
    }
    public List<BookingResponse> mapToBookingResponseList(List<Booking> bookings) {
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    private void mapBasicBookingFields(BookingResponse response, Booking booking) {
        response.setId(booking.getId());
        response.setCheckInDate(booking.getCheckInDate());
        response.setCheckOutDate(booking.getCheckOutDate());
        response.setNumberOfGuests(booking.getNumberOfGuests());
        response.setBookingDate(booking.getBookingDate());
        response.setTotalPrice(booking.getTotalPrice().doubleValue());
        response.setSpecialRequests(booking.getSpecialRequests());
        response.setStatus(booking.getStatus());

        if (booking.getAssignedRoom() != null) {
            response.setAssignedRoomId(booking.getAssignedRoom().getId());
            response.setRoomNumber(booking.getAssignedRoom().getRoomNumber());
        }
    }
    private void mapDepositInfo(BookingResponse response, Booking booking) {
        // Map from booking entity
        response.setDepositAmount(booking.getDepositAmount() != null ?
                booking.getDepositAmount().doubleValue() : null);
        response.setRemainingAmount(booking.getRemainingAmount() != null ?
                booking.getRemainingAmount().doubleValue() : null);

        // Find deposit payment to get more details
        if (booking.getPayments() != null && !booking.getPayments().isEmpty()) {
            Payment depositPayment = booking.getPayments().stream()
                    .filter(p -> Payment.PaymentType.COC_TRUOC.equals(p.getPaymentType()))
                    .filter(Payment::isPaid)
                    .findFirst()
                    .orElse(null);

            if (depositPayment != null) {
                response.setPaymentType(depositPayment.getPaymentType());
                response.setDepositPercentage(depositPayment.getDepositPercentage() != null ?
                        depositPayment.getDepositPercentage().doubleValue() : null);
            }

            // Check for any paid payment
            Payment anyPaidPayment = booking.getPayments().stream()
                    .filter(Payment::isPaid)
                    .findFirst()
                    .orElse(null);

            if (anyPaidPayment != null) {
                response.setPaymentType(anyPaidPayment.getPaymentType());
            }
        }
    }


    private void mapUserInfo(BookingResponse response, User user) {
        response.setUserId(user.getId());
        response.setUserName(user.getFullName());
        response.setUserEmail(user.getEmail());
    }

    private void mapRoomTypeInfo(BookingResponse response, RoomType roomType) {
        response.setRoomTypeId(roomType.getId());
        response.setRoomTypeName(roomType.getTypeName());
        response.setRoomTypeBasePrice(roomType.getBasePrice() != null ? roomType.getBasePrice().doubleValue() : null);
        response.setRoomTypeMaxOccupancy(roomType.getMaxOccupancy());
        response.setRoomTypeDescription(roomType.getDescription());

        if (roomType.getAmenities() != null && !roomType.getAmenities().isEmpty()) {
            String amenitiesString = roomType.getAmenities().stream()
                    .map(Amenity::getAmenityName)
                    .collect(Collectors.joining(", "));
            response.setRoomTypeAmenities(amenitiesString);
        }
    }

    private void mapReviewInfo(BookingResponse response, Booking booking) {
        if (booking.getUser() != null && booking.getRoomType() != null && booking.getRoomType().getHotel() != null) {
            Long userId = booking.getUser().getId();
            Long hotelId = booking.getRoomType().getHotel().getId();

            // Check if user has completed this booking
            boolean isCompleted = BookingStatus.COMPLETED.equals(booking.getStatus());

            // Check if user already reviewed this hotel
            boolean hasReviewed = reviewRepository.existsByUserIdAndHotelId(userId, hotelId);

            // Set review eligibility: can review if completed and not yet reviewed
            response.setCanReview(isCompleted && !hasReviewed);
            response.setHasReviewed(hasReviewed);

            // Get existing review ID if exists
            if (hasReviewed) {
                Review existingReview = reviewRepository.findByUserIdAndHotelId(userId, hotelId);
                if (existingReview != null) {
                    response.setExistingReviewId(existingReview.getId());
                }
            }
        } else {
            // Default values if data is missing
            response.setCanReview(false);
            response.setHasReviewed(false);
            response.setExistingReviewId(null);
        }
    }

    private void mapHotelInfo(BookingResponse response, Hotel hotel) {
        if (hotel == null) return;

        response.setHotelId(hotel.getId());
        response.setHotelName(hotel.getHotelName());
        response.setHotelAddress(hotel.getAddress());
        response.setHotelStarRating(hotel.getStarRating());
        response.setHotelPhoneNumber(hotel.getPhoneNumber());
        response.setHotelEmail(hotel.getEmail());
        response.setHotelWebsite(hotel.getWebsite());
        response.setIsHotelActive(hotel.getIsActive());
        response.setHotelPropertyType(hotel.getPropertyType());

        if (hotel.getLocation() != null) {
            response.setHotelLocationCity(hotel.getLocation().getCityName());
            response.setHotelLocationDistrict(hotel.getLocation().getCityName());
        }

        if (hotel.getReviews() != null && !hotel.getReviews().isEmpty()) {
            Double avgRating = hotel.getReviews().stream()
                    .mapToDouble(review -> review.getRating().doubleValue())
                    .average()
                    .orElse(0.0);
            response.setHotelAverageRating(avgRating);
            response.setHotelReviewCount(hotel.getReviews().size());
        } else {
            response.setHotelAverageRating(0.0);
            response.setHotelReviewCount(0);
        }

        if (hotel.getImages() != null && !hotel.getImages().isEmpty()) {
            List<HotelImageResponse> imageResponses = hotel.getImages().stream()
                    .map(this::mapToHotelImageResponse)
                    .collect(Collectors.toList());
            response.setHotelImages(imageResponses);
        }
    }

    private HotelImageResponse mapToHotelImageResponse(HotelImage image) {
        HotelImageResponse imageResponse = new HotelImageResponse();
        imageResponse.setId(image.getId());
        imageResponse.setHotelId(image.getHotel().getId());
        imageResponse.setImageUrl(image.getImageUrl());
        imageResponse.setCaption(image.getCaption());
        imageResponse.setIsPrimary(image.getIsPrimary());
        imageResponse.setHotelName(image.getHotel().getHotelName());
        return imageResponse;
    }

    private void mapPaymentInfo(BookingResponse response, Set<Payment> payments) {
        if (payments != null && !payments.isEmpty()) {
            // Get latest payment
            Payment latestPayment = payments.stream()
                    .max((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                    .orElse(null);

            if (latestPayment != null) {
                response.setPaymentId(latestPayment.getId());
                response.setPaymentStatus(latestPayment.getPaymentStatus());
                response.setPaymentMethod(latestPayment.getPaymentMethod());
                response.setPaymentDate(latestPayment.getPaymentDate());
                response.setIsPaid(latestPayment.isPaid());
                response.setQrCode(latestPayment.getQrCode());

                // ✅ ADD: Set payment type from latest payment
                response.setPaymentType(latestPayment.getPaymentType());
            }
        } else {
            response.setPaymentStatus("Chưa thanh toán");
            response.setIsPaid(false);
        }
    }

    private void mapPermissions(BookingResponse response, Booking booking) {
        response.setCanCancel(calculateCanCancel(booking));
        response.setCanModify(calculateCanModify(booking));
        response.setCanCheckIn(calculateCanCheckIn(booking));
        response.setCanCheckOut(calculateCanCheckOut(booking));

        // ✅ ADD: Calculate deposit-specific permissions
        response.setCanPayRemaining(calculateCanPayRemaining(booking));
        response.setIsFullyPaid(calculateIsFullyPaid(booking));
        response.setIsDepositPayment(calculateIsDepositPayment(booking));
    }

    private Boolean calculateCanPayRemaining(Booking booking) {
        return BookingStatus.PAID.equals(booking.getStatus()) &&
                booking.getRemainingAmount() != null &&
                booking.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private Boolean calculateIsFullyPaid(Booking booking) {
        return booking.getRemainingAmount() == null ||
                booking.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0 ||
                BookingStatus.CONFIRMED.equals(booking.getStatus()) ||
                BookingStatus.CHECKED_IN.equals(booking.getStatus()) ||
                BookingStatus.COMPLETED.equals(booking.getStatus());
    }

    private Boolean calculateIsDepositPayment(Booking booking) {
        // Check if has deposit amount and remaining amount
        boolean hasDeposit = booking.getDepositAmount() != null &&
                booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean hasRemaining = booking.getRemainingAmount() != null &&
                booking.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;

        return hasDeposit && hasRemaining && BookingStatus.PAID.equals(booking.getStatus());
    }
    private Boolean calculateCanCancel(Booking booking) {
        String status = booking.getStatus();

        // ✅ Cannot cancel these statuses
        if (BookingStatus.CANCELLED.equals(status) ||
                BookingStatus.COMPLETED.equals(status) ||
                BookingStatus.CHECKED_IN.equals(status)) {
            return false;
        }

        // ✅ Can always cancel unpaid bookings
        if (BookingStatus.TEMPORARY.equals(status) ||
                BookingStatus.PENDING.equals(status)) {
            return true;
        }

        // ✅ For PAID and CONFIRMED status, check time limit
        if (BookingStatus.PAID.equals(status) ||           // ✅ THÊM
                BookingStatus.CONFIRMED.equals(status) ||
                BookingStatus.DEPOSIT_PAID.equals(status)) {

            LocalDate checkInDate = booking.getCheckInDate();
            LocalDate now = LocalDate.now();
            long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

            return daysUntilCheckIn >= 1; // Allow cancel if >= 24 hours before check-in
        }

        return false;
    }

    private Boolean calculateCanModify(Booking booking) {
        String status = booking.getStatus();
        if (BookingStatus.CANCELLED.equals(status) ||
                BookingStatus.COMPLETED.equals(status) ||
                BookingStatus.CHECKED_IN.equals(status)) {
            return false;
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        return daysUntilCheckIn >= 2;
    }

    // ✅ SỬA BookingMappingService
// ✅ SỬA BookingMappingService
    private Boolean calculateCanCheckIn(Booking booking) {
        String status = booking.getStatus();

        if (!BookingStatus.CONFIRMED.equals(status) && !BookingStatus.PAID.equals(status)) {
            return false;
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate today = LocalDate.now();

        // ✅ FULL PAYMENT: Không giới hạn thời gian
        if (BookingStatus.CONFIRMED.equals(status)) {
            // Đã thanh toán full → Luôn có thể check-in
            return true; // Unlimited grace period
        }

        // ✅ DEPOSIT PAYMENT: Có giới hạn thời gian
        if (BookingStatus.PAID.equals(status)) {
            // Current or future dates
            if (checkInDate.equals(today) || checkInDate.isAfter(today)) {
                return true;
            }

            // Past dates - Limited grace period cho deposit
            if (checkInDate.isBefore(today)) {
                long daysPassed = ChronoUnit.DAYS.between(checkInDate, today);
                return daysPassed <= CHECK_IN_GRACE_DAYS; // 1 ngày
            }
        }

        return false;
    }

    private Boolean calculateCanCheckOut(Booking booking) {
        String status = booking.getStatus();

        if (!BookingStatus.CHECKED_IN.equals(status)) {
            return false;
        }

        LocalDate checkOutDate = booking.getCheckOutDate();
        LocalDate now = LocalDate.now();

        return now.isBefore(checkOutDate) || now.equals(checkOutDate);
    }
}