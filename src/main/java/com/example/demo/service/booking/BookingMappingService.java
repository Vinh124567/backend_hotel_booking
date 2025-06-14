package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.entity.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingMappingService {

    public BookingResponse mapToBookingResponse(Booking booking) {
        BookingResponse response = new BookingResponse();

        mapBasicBookingFields(response, booking);
        mapUserInfo(response, booking.getUser());
        mapRoomTypeInfo(response, booking.getRoomType());
        mapHotelInfo(response, booking.getRoomType().getHotel());
        mapPaymentInfo(response, booking.getPayments());
        mapPermissions(response, booking);

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
            Payment payment = payments.iterator().next();
            response.setPaymentId(payment.getId());
            response.setPaymentStatus(payment.getPaymentStatus());
            response.setPaymentMethod(payment.getPaymentMethod());
            response.setPaymentDate(payment.getPaymentDate());
            response.setIsPaid(payment.isPaid());
            response.setQrCode(payment.getQrCode());
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
    }

    private Boolean calculateCanCancel(Booking booking) {
        String status = booking.getStatus();

        if (BookingStatus.CANCELLED.equals(status) || BookingStatus.COMPLETED.equals(status) || BookingStatus.CHECKED_IN.equals(status)) {
            return false;
        }

        Set<Payment> payments = booking.getPayments();
        if (payments != null && !payments.isEmpty()) {
            Payment payment = payments.iterator().next();
            if (payment.isPaid()) {
                return false;
            }
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        return daysUntilCheckIn >= 1;
    }

    private Boolean calculateCanModify(Booking booking) {
        String status = booking.getStatus();
        if (BookingStatus.CANCELLED.equals(status) || BookingStatus.COMPLETED.equals(status) || BookingStatus.CHECKED_IN.equals(status)) {
            return false;
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        return daysUntilCheckIn >= 2;
    }

    private Boolean calculateCanCheckIn(Booking booking) {
        String status = booking.getStatus();

        if (!BookingStatus.TEMPORARY.equals(status) && !BookingStatus.PENDING.equals(status) && !BookingStatus.CONFIRMED.equals(status)) {
            return false;
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();

        return now.equals(checkInDate) || now.isAfter(checkInDate);
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