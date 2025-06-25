package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatsResponse;
import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.dto.report.HotelStatsResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final int TEMPORARY_BOOKING_EXPIRE_MINUTES = 15;
    private static final int PENDING_BOOKING_EXPIRE_MINUTES = 30;
    private static final long TOTAL_ROOMS_PER_TYPE = 5;
    private final RoomRepository roomRepository;
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
        validationService.validateUserBookingLimits(currentUser.getId());
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại với ID: " + request.getRoomTypeId()));

//        if (availabilityService.hasUserPendingBookingForDates(currentUser.getId(),
//                request.getCheckInDate(), request.getCheckOutDate())) {
//            throw new RuntimeException("Bạn đã có booking đang chờ thanh toán cho thời gian này");
//        }

        // ✅ THÊM: Handle 2 flows khác nhau
        Room assignedRoom = null;

        if (request.hasSpecificRoomSelected()) {
            // Flow 1: User chọn phòng cụ thể
            assignedRoom = validateAndGetSpecificRoom(request);
            log.info("User selected specific room: {}", assignedRoom.getRoomNumber());
        } else {
            // Flow 2: User chỉ chọn roomType, check availability tổng quát
            if (!availabilityService.isRoomTypeAvailable(request.getRoomTypeId(),
                    request.getCheckInDate(), request.getCheckOutDate())) {
                throw new RuntimeException("Loại phòng không khả dụng trong thời gian đã chọn");
            }
            log.info("User selected room type: {}, system will auto-assign room later", roomType.getTypeName());
        }

        Booking booking = createBookingEntity(currentUser, roomType, request);

        // ✅ Assign room ngay nếu user đã chọn cụ thể
        if (assignedRoom != null) {
            booking.setAssignedRoom(assignedRoom);
        }

        booking = bookingRepository.save(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    private Room validateAndGetSpecificRoom(BookingRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại với ID: " + request.getRoomId()));

        // Validate room thuộc đúng roomType
        if (!room.getRoomType().getId().equals(request.getRoomTypeId())) {
            throw new RuntimeException("Phòng không thuộc loại phòng đã chọn");
        }

        // Validate room có available không
        if (!availabilityService.isSpecificRoomAvailable(request.getRoomId(),
                request.getCheckInDate(), request.getCheckOutDate())) {
            throw new RuntimeException("Phòng " + room.getRoomNumber() + " không khả dụng trong thời gian đã chọn");
        }

        return room;
    }

    @Override
    public boolean isRoomTypeAvailable(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        return availabilityService.isRoomTypeAvailable(roomTypeId, checkInDate, checkOutDate);
    }

    @Transactional
    @Override
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        validationService.validateBookingForConfirmation(booking);

        if (!availabilityService.isRoomTypeAvailableForConfirmation(booking)) {
            throw new RuntimeException("Phòng không còn khả dụng");
        }

        // ✅ THÊM: Assign room cụ thể khi confirm
        if (booking.getAssignedRoom() == null) {
            Room availableRoom = availabilityService.findAvailableRoom(
                    booking.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );

            if (availableRoom == null) {
                throw new RuntimeException("Không có phòng trống để assign");
            }

            booking.setAssignedRoom(availableRoom);
            log.info("Assigned room {} to booking {}", availableRoom.getRoomNumber(), bookingId);
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
        Room assignedRoom = booking.getAssignedRoom();
        if (assignedRoom != null) {
            assignedRoom.setStatus("Trống"); // hoặc RoomStatus.AVAILABLE
            roomRepository.save(assignedRoom);

            log.info("Updated room {} status to 'Trống' on booking cancellation",
                    assignedRoom.getRoomNumber());
        }
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

    @Transactional
    @Override
    public BookingResponse checkInBooking(Long bookingId) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        validationService.validateBookingOwnership(booking, currentUser);
        validationService.validateCheckIn(booking);

        // ✅ THÊM: Đảm bảo có room được assign trước khi check-in
        if (booking.getAssignedRoom() == null) {
            Room availableRoom = availabilityService.findAvailableRoom(
                    booking.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );

            if (availableRoom == null) {
                throw new RuntimeException("Không có phòng trống để check-in");
            }

            booking.setAssignedRoom(availableRoom);
            log.info("Auto-assigned room {} for check-in booking {}", availableRoom.getRoomNumber(), bookingId);
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        Room assignedRoom = booking.getAssignedRoom();
        if (assignedRoom != null) {
            assignedRoom.setStatus("Đang sử dụng");
            roomRepository.save(assignedRoom);

            log.info("Updated room {} status to 'Đang sử dụng' on check-in",
                    assignedRoom.getRoomNumber());
        }
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
        Room assignedRoom = booking.getAssignedRoom();
        if (assignedRoom != null) {
            assignedRoom.setStatus("Trống"); // hoặc RoomStatus.AVAILABLE
            roomRepository.save(assignedRoom);

            log.info("Updated room {} status to 'Trống' on check-out",
                    assignedRoom.getRoomNumber());
        }
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

            if (hasSuccessfulPayment) {
                booking.setStatus(BookingStatus.CONFIRMED);

                // ✅ THÊM: Auto-assign room khi payment success
                if (booking.getAssignedRoom() == null) {
                    Room availableRoom = availabilityService.findAvailableRoom(
                            booking.getRoomType().getId(),
                            booking.getCheckInDate(),
                            booking.getCheckOutDate()
                    );
                    if (availableRoom != null) {
                        booking.setAssignedRoom(availableRoom);
                        log.info("Auto-assigned room {} to paid booking {}",
                                availableRoom.getRoomNumber(), booking.getId());
                    }
                }
            } else {
                booking.setStatus(BookingStatus.CANCELLED);
            }
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

    private void assignRoomIfNeeded(Booking booking) {
        if (booking.getAssignedRoom() == null) {
            Room availableRoom = availabilityService.findAvailableRoom(
                    booking.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );

            if (availableRoom != null) {
                booking.setAssignedRoom(availableRoom);
                log.info("Auto-assigned room {} to booking {}",
                        availableRoom.getRoomNumber(), booking.getId());
            } else {
                log.warn("No available room for booking {}", booking.getId());
            }
        }
    }

    @Override
    public List<BookingResponse> getBookingsByHotel(Long hotelId, String status) {
        List<Booking> bookings;

        if (status != null && !status.trim().isEmpty()) {
            bookings = bookingRepository.findByRoomType_Hotel_IdAndStatusOrderByBookingDateDesc(hotelId, status);
        } else {
            bookings = bookingRepository.findByRoomType_Hotel_IdOrderByBookingDateDesc(hotelId);
        }

        return bookings.stream()
                .map(mappingService::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HotelStatsResponse getHotelRevenue(Long hotelId, LocalDate fromDate, LocalDate toDate, String status) {
        // Tổng doanh thu
        BigDecimal totalRevenue = bookingRepository.calculateTotalRevenue(hotelId, fromDate, toDate, status);

        // Doanh thu theo tháng
        List<Object[]> monthlyData = bookingRepository.findMonthlyRevenue(hotelId, fromDate, toDate, status);

        List<HotelStatsResponse.MonthlyRevenue> monthlyRevenues = monthlyData.stream()
                .map(row -> {
                    Integer year = (Integer) row[0];
                    Integer month = (Integer) row[1];
                    Long bookings = (Long) row[2];
                    BigDecimal revenue = (BigDecimal) row[3];

                    return HotelStatsResponse.MonthlyRevenue.builder()
                            .month(String.format("%d-%02d", year, month))
                            .displayMonth(String.format("Tháng %d/%d", month, year))
                            .revenue(revenue)
                            .bookings(bookings)
                            .build();
                })
                .collect(Collectors.toList());

        // Tính stats cơ bản
        Long totalBookings = monthlyRevenues.stream().mapToLong(HotelStatsResponse.MonthlyRevenue::getBookings).sum();
        Long completedBookings = bookingRepository.calculateTotalRevenue(hotelId, fromDate, toDate, "Hoàn thành").equals(BigDecimal.ZERO) ? 0L : totalBookings;
        Long cancelledBookings = bookingRepository.calculateTotalRevenue(hotelId, fromDate, toDate, "Đã hủy").equals(BigDecimal.ZERO) ? 0L : 0L;

        return HotelStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .monthlyRevenues(monthlyRevenues)
                .build();
    }


}