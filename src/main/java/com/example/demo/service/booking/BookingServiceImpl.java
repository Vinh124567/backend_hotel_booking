package com.example.demo.service.booking;
import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatsResponse;
import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.entity.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final int TEMPORARY_BOOKING_EXPIRE_MINUTES = 15;
    private static final int PENDING_BOOKING_EXPIRE_MINUTES = 30;
    private static final long TOTAL_ROOMS_PER_TYPE = 5;

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingAvailabilityService availabilityService;
    private final BookingValidationService validationService;
    private final BookingMappingService mappingService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<BookingResponse> getCurrentUserPendingBookings() {
        User currentUser = userService.getCurrentUser();

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(TEMPORARY_BOOKING_EXPIRE_MINUTES);

        List<Booking> pendingBookings = bookingRepository
                .findUserTemporaryBookingsNotExpired(currentUser.getId(), cutoffTime);

        return mappingService.mapToBookingResponseList(pendingBookings);
    }

    @Transactional
    @Override
    public BookingResponse createBooking(BookingRequest request) {
        User currentUser = userService.getCurrentUser();

        validationService.validateBookingRequest(request);

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại với ID: " + request.getRoomTypeId()));

        if (availabilityService.hasUserPendingBookingForDates(currentUser.getId(),
                request.getCheckInDate(), request.getCheckOutDate())) {
            throw new RuntimeException("Bạn đã có booking đang chờ thanh toán cho thời gian này");
        }

        if (!availabilityService.isRoomTypeAvailable(request.getRoomTypeId(),
                request.getCheckInDate(), request.getCheckOutDate())) {
            throw new RuntimeException("Loại phòng không khả dụng trong thời gian đã chọn");
        }

        Booking booking = createBookingEntity(currentUser, roomType, request);
        booking = bookingRepository.save(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    @Override
    public boolean isRoomTypeAvailable(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        return availabilityService.isRoomTypeAvailable(roomTypeId, checkInDate, checkOutDate);
    }

    @Override
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        validationService.validateBookingForConfirmation(booking);

        if (!availabilityService.isRoomTypeAvailableForConfirmation(booking)) {
            throw new RuntimeException("Phòng không còn khả dụng");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    @Override
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        User currentUser = userService.getCurrentUser();
        validationService.validateBookingOwnership(booking, currentUser);
        validationService.validateCancellation(booking);

        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    @Override
    public List<BookingResponse> getPendingBookings() {
        List<Booking> pendingBookings = bookingRepository.findByStatusWithDetailsOrderByBookingDateDesc(BookingStatus.PENDING);
        return mappingService.mapToBookingResponseList(pendingBookings);
    }

    @Override
    public List<BookingResponse> getCurrentUserBookings() {
        User currentUser = userService.getCurrentUser();
        List<Booking> bookings = bookingRepository.findByUserIdWithDetailsOrderByBookingDateDesc(currentUser.getId());
        return mappingService.mapToBookingResponseList(bookings);
    }

    @Override
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));
        return mappingService.mapToBookingResponse(booking);
    }

    @Override
    public BookingResponse checkInBooking(Long bookingId) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        validationService.validateBookingOwnership(booking, currentUser);
        validationService.validateCheckIn(booking);

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking = bookingRepository.save(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    @Override
    public BookingResponse checkOutBooking(Long bookingId) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        validationService.validateBookingOwnership(booking, currentUser);
        validationService.validateCheckOut(booking);

        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    @Override
    public BookingResponse updateBooking(Long bookingId, BookingRequest request) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        validationService.validateBookingOwnership(booking, currentUser);
        validationService.validateModification(booking);

        updateBookingFields(booking, request);
        booking = bookingRepository.save(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    @Override
    public BookingStatsResponse getUserBookingStats() {
        User currentUser = userService.getCurrentUser();
        List<Booking> userBookings = bookingRepository.findByUserId(currentUser.getId());

        return BookingStatsResponse.builder()
                .totalBookings((long) userBookings.size())
                .activeBookings(countBookingsByStatus(userBookings, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN))
                .completedBookings(countBookingsByStatus(userBookings, BookingStatus.COMPLETED))
                .cancelledBookings(countBookingsByStatus(userBookings, BookingStatus.CANCELLED))
                .totalSpent(calculateTotalSpent(userBookings))
                .favoriteHotels((long) currentUser.getFavoriteHotels().size())
                .build();
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredTemporaryBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(TEMPORARY_BOOKING_EXPIRE_MINUTES);
        List<Booking> expiredBookings = bookingRepository.findExpiredTemporaryBookings(cutoffTime);

        expiredBookings.forEach(booking -> {
            boolean hasSuccessfulPayment = booking.getPayments().stream()
                    .anyMatch(Payment::isPaid);

            booking.setStatus(hasSuccessfulPayment ? BookingStatus.CONFIRMED : BookingStatus.CANCELLED);
        });

        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);
        }
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cleanupOldPendingBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(PENDING_BOOKING_EXPIRE_MINUTES);
        List<Booking> oldPendingBookings = bookingRepository.findOldPendingBookingsWithoutPayment(cutoffTime);

        oldPendingBookings.forEach(booking -> booking.setStatus(BookingStatus.CANCELLED));

        if (!oldPendingBookings.isEmpty()) {
            bookingRepository.saveAll(oldPendingBookings);
        }
    }

    private Booking createBookingEntity(User user, RoomType roomType, BookingRequest request) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoomType(roomType);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setTotalPrice(BigDecimal.valueOf(request.getTotalPrice()));
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setStatus(BookingStatus.TEMPORARY);
        return booking;
    }

    private void updateBookingFields(Booking booking, BookingRequest request) {
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setTotalPrice(BigDecimal.valueOf(request.getTotalPrice()));
        booking.setSpecialRequests(request.getSpecialRequests());
    }

    private long countBookingsByStatus(List<Booking> bookings, String... statuses) {
        return bookings.stream()
                .filter(b -> List.of(statuses).contains(b.getStatus()))
                .count();
    }



    private Double calculateTotalSpent(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> !BookingStatus.CANCELLED.equals(b.getStatus()))
                .mapToDouble(b -> b.getTotalPrice().doubleValue())
                .sum();
    }

    @Override
    public List<BookingResponse> getBookingsReadyForCheckIn() {
        List<BookingResponse> allBookings = getCurrentUserBookings();
        return allBookings.stream()
                .filter(booking -> Boolean.TRUE.equals(booking.getCanCheckIn()))
                .toList();
    }

    @Override
    public List<BookingResponse> getBookingsReadyForCheckOut() {
        List<BookingResponse> allBookings = getCurrentUserBookings();
        return allBookings.stream()
                .filter(booking -> Boolean.TRUE.equals(booking.getCanCheckOut()))
                .toList();
    }

    @Override
    public List<BookingResponse> getCurrentlyCheckedInBookings() {
        List<BookingResponse> allBookings = getCurrentUserBookings();
        return allBookings.stream()
                .filter(BookingResponse::isCheckedIn)
                .toList();
    }

    public List<BookingResponse> getCheckHistory() {
        List<BookingResponse> allBookings = getCurrentUserBookings();
        return allBookings.stream()
                .filter(booking -> booking.isCheckedIn() || booking.isCheckedOut())
                .toList();
    }
}