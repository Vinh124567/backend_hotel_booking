package com.example.demo.service.booking;
import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatsResponse;
import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.dto.report.HotelStatsResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.service.notification.AdminNotificationService;
import com.example.demo.service.payment.PaymentService;
import com.example.demo.service.user.UserService;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingAvailabilityService availabilityService;
    private final BookingValidationService validationService;
    private final BookingMappingService mappingService;
    private final AdminNotificationService adminNotificationService;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    // ========== EXISTING CORE METHODS (KEEP AS IS) ==========

    @Override
    public List<BookingResponse> getCurrentUserPendingBookings() {
        User currentUser = userService.getCurrentUser();
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(TEMPORARY_BOOKING_EXPIRE_MINUTES);
        List<Booking> pendingBookings = bookingRepository
                .findUserTemporaryBookingsNotExpired(currentUser.getId(), cutoffTime);
        return mappingService.mapToBookingResponseList(pendingBookings);
    }

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Override
    public BookingResponse createBooking(BookingRequest request) {
        User currentUser = userService.getCurrentUser();

        validationService.validateBookingRequest(request);
        validationService.validateUserBookingLimits(currentUser.getId());

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại với ID: " + request.getRoomTypeId()));

        Room assignedRoom = null;

        if (request.hasSpecificRoomSelected()) {
            assignedRoom = validateAndGetSpecificRoom(request);
            log.info("User selected specific room: {}", assignedRoom.getRoomNumber());
        } else {
            if (!availabilityService.isRoomTypeAvailable(request.getRoomTypeId(),
                    request.getCheckInDate(), request.getCheckOutDate())) {
                throw new RuntimeException("Loại phòng không khả dụng trong thời gian đã chọn");
            }
            log.info("User selected room type: {}, system will auto-assign room later", roomType.getTypeName());
        }

        // Create booking entity
        Booking booking = createBookingEntity(currentUser, roomType, request);
        if (assignedRoom != null) {
            booking.setAssignedRoom(assignedRoom);
        }

        // Handle deposit payment logic
        if (request.wantsDepositPayment()) {
            booking.setDepositPercentage(BigDecimal.valueOf(request.getDepositPercentage()));
            booking.setStatus(BookingStatus.TEMPORARY);
            booking.setDepositAmount(request.getDepositAmount());
            booking.setRemainingAmount(request.getRemainingAmount());
            log.info("Created deposit booking: {}% = {} VND", request.getDepositPercentage(), request.getDepositAmount());
        } else {
            booking.setStatus(BookingStatus.TEMPORARY);
            log.info("Created full payment booking: {} VND", request.getTotalPrice());
        }

        booking = bookingRepository.save(booking);
        return mappingService.mapToBookingResponse(booking);
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

        User currentUser = userService.getCurrentUser();
        boolean isAdmin = isUserAdmin(currentUser);

        if (!isAdmin) {
            validationService.validateBookingOwnership(booking, currentUser);
        }

        return mappingService.mapToBookingResponse(booking);
    }

    @Override
    public BookingResponse updateBooking(Long bookingId, BookingRequest request) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        boolean isAdmin = isUserAdmin(currentUser);
        if (!isAdmin) {
            validationService.validateBookingOwnership(booking, currentUser);
        }

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
                .activeBookings(countBookingsByStatus(userBookings, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.PAID))
                .completedBookings(countBookingsByStatus(userBookings, BookingStatus.COMPLETED))
                .cancelledBookings(countBookingsByStatus(userBookings, BookingStatus.CANCELLED))
                .totalSpent(calculateTotalSpent(userBookings))
                .favoriteHotels((long) currentUser.getFavoriteHotels().size())
                .build();
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
        BigDecimal totalRevenue = bookingRepository.calculateTotalRevenue(hotelId, fromDate, toDate, status);

        List<Object[]> monthlyData = bookingRepository.findMonthlyRevenue(hotelId, fromDate, toDate, status);

        List<HotelStatsResponse.MonthlyRevenue> monthlyRevenues = monthlyData.stream()
                .map(row -> {
                    Integer year = (Integer) row[0];
                    Integer month = (Integer) row[1];
                    Long bookingsCount = (Long) row[2];
                    BigDecimal revenue = (BigDecimal) row[3];

                    return HotelStatsResponse.MonthlyRevenue.builder()
                            .month(String.format("%d-%02d", year, month))
                            .displayMonth(String.format("Tháng %d/%d", month, year))
                            .revenue(revenue)
                            .bookings(bookingsCount)
                            .build();
                })
                .collect(Collectors.toList());

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

    // ========== 🆕 NEW DEPOSIT PAYMENT METHODS ==========

    // ✅ Code đầy đủ logic
    @Transactional
    @Override
    public BookingResponse payDeposit(Long bookingId, BigDecimal depositPercentage) {

        // ✅ DEBUG
        System.out.println("=== BEFORE PROCESSING ===");
        System.out.println("Input depositPercentage: " + depositPercentage);
        log.info("Input depositPercentage: {}");
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));

        // ✅ VALIDATION THỰC TẾ
        if (!BookingStatus.TEMPORARY.equals(booking.getStatus()) &&
                !BookingStatus.PENDING.equals(booking.getStatus())) {
            throw new RuntimeException("Chỉ có thể thanh toán cọc cho booking ở trạng thái phù hợp");
        }

        BigDecimal minDeposit = new BigDecimal("10");
        BigDecimal maxDeposit = new BigDecimal("50");

        if (depositPercentage.compareTo(minDeposit) < 0 || depositPercentage.compareTo(maxDeposit) > 0) {
            throw new RuntimeException("Tỷ lệ cọc phải từ 10% đến 50%");
        }

        // ✅ TÍNH TOÁN DEPOSIT AMOUNT VÀ REMAINING
        BigDecimal hundred = new BigDecimal("100");
        BigDecimal depositAmount = booking.getTotalPrice()
                .multiply(depositPercentage)
                .divide(hundred, 2, RoundingMode.HALF_UP);

        BigDecimal remainingAmount = booking.getTotalPrice()
                .subtract(depositAmount)
                .setScale(2, RoundingMode.HALF_UP);

        // ✅ CREATE PAYMENT USING PAYMENT SERVICE
        Payment depositPayment = paymentService.createDepositPayment(
                bookingId,
                depositAmount,
                depositPercentage
        );

        // ✅ UPDATE BOOKING WITH ALL FIELDS
        booking.setStatus(BookingStatus.PAID); // "Đã thanh toán"
        booking.setDepositPercentage(depositPercentage);
        booking.setDepositAmount(depositAmount);
        booking.setRemainingAmount(remainingAmount);
        log.info("🏦 [DEBUG] Booking saved started. About to notify admin...");
        booking = bookingRepository.save(booking);
        log.info("🏦 [DEBUG] Booking saved successfully. About to notify admin...");

        // ✅ NOTIFY ADMIN
        adminNotificationService.notifyDepositPayment(booking, depositPayment);

        log.info("Deposit payment completed for booking {}: {} VND ({}%)",
                bookingId, depositAmount, depositPercentage);

        return mappingService.mapToBookingResponse(booking);
    }

    @Transactional
    @Override
    public BookingResponse payRemaining(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));

        // ✅ Check for paid status with remaining amount
        if (!BookingStatus.PAID.equals(booking.getStatus())) {
            throw new RuntimeException("Booking phải ở trạng thái 'Đã thanh toán' với số tiền còn lại");
        }

        if (booking.getRemainingAmount() == null || booking.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Không có số tiền còn lại để thanh toán");
        }

        // Create remaining payment using PaymentService
        Payment remainingPayment = paymentService.createRemainingPayment(
                bookingId,
                booking.getRemainingAmount()
        );

        // Update booking to confirmed (fully paid)
        booking.setStatus(BookingStatus.CONFIRMED); // "Đã xác nhận"
        booking.setRemainingAmount(BigDecimal.ZERO);

        // Auto-assign room if needed
        if (booking.getAssignedRoom() == null) {
            Room availableRoom = availabilityService.findAvailableRoom(
                    booking.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );
            if (availableRoom != null) {
                booking.setAssignedRoom(availableRoom);
                log.info("Auto-assigned room {} to confirmed booking {}",
                        availableRoom.getRoomNumber(), bookingId);
            }
        }

        booking = bookingRepository.save(booking);

        // Notify admin
        adminNotificationService.notifyFullPayment(booking, remainingPayment);

        log.info("Remaining payment completed for booking {}: {} VND",
                bookingId, remainingPayment.getAmount());

        return mappingService.mapToBookingResponse(booking);
    }

    @Transactional
    @Override
    public BookingResponse payFullAmount(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking không tồn tại"));
        log.info("💯 [DEBUG] PayFullAmount method called for booking: {}", bookingId);
        if (!BookingStatus.TEMPORARY.equals(booking.getStatus()) &&
                !BookingStatus.PENDING.equals(booking.getStatus())) {
            throw new RuntimeException("Chỉ có thể thanh toán cho booking ở trạng thái phù hợp");
        }

        // Create full payment using PaymentService
        Payment fullPayment = paymentService.createFullPayment(
                bookingId,
                booking.getTotalPrice()
        );

        // Update booking to confirmed (fully paid)
        booking.setStatus(BookingStatus.CONFIRMED); // "Đã xác nhận"
        booking.setDepositAmount(BigDecimal.ZERO);
        booking.setRemainingAmount(BigDecimal.ZERO);

        // Auto-assign room if needed
        if (booking.getAssignedRoom() == null) {
            Room availableRoom = availabilityService.findAvailableRoom(
                    booking.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate()
            );
            if (availableRoom != null) {
                booking.setAssignedRoom(availableRoom);
                log.info("Auto-assigned room {} to confirmed booking {}",
                        availableRoom.getRoomNumber(), bookingId);
            }
        }

        booking = bookingRepository.save(booking);

        // Notify admin
        adminNotificationService.notifyFullPayment(booking, fullPayment);

        log.info("Full payment completed for booking {}: {} VND",
                bookingId, fullPayment.getAmount());

        return mappingService.mapToBookingResponse(booking);
    }

    // ========== UPDATED CANCELLATION WITH ADMIN NOTIFICATION ==========

    @Override
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        User currentUser = userService.getCurrentUser();

        boolean isAdmin = isUserAdmin(currentUser);
        if (!isAdmin) {
            validationService.validateBookingOwnership(booking, currentUser);
        }

        validationService.validateCancellation(booking);

        // ✅ Notify admin BEFORE changing status if it's a confirmed/paid booking
        String oldStatus = booking.getStatus();
        if (BookingStatus.requiresAdminNotification(oldStatus)) {
            adminNotificationService.notifyBookingCancellation(booking, currentUser);
        }

        // ✅ Handle refund processing
        handleCancellationRefund(booking);

        booking.setStatus(BookingStatus.CANCELLED);

        Room assignedRoom = booking.getAssignedRoom();
        if (assignedRoom != null) {
            assignedRoom.setStatus("Trống");
            roomRepository.save(assignedRoom);
            log.info("Updated room {} status to 'Trống' on booking cancellation",
                    assignedRoom.getRoomNumber());
        }

        booking = bookingRepository.save(booking);

        log.info("Booking {} cancelled by {} (was: {})", bookingId,
                currentUser.getUsername(), oldStatus);

        return mappingService.mapToBookingResponse(booking);
    }

    // ========== UPDATED CHECK-IN/OUT WITH ADMIN NOTIFICATION ==========

    @Transactional
    @Override
    public BookingResponse checkInBooking(Long bookingId) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        boolean isAdmin = isUserAdmin(currentUser);
        if (!isAdmin) {
            throw new RuntimeException("Chỉ admin mới có thể thực hiện check-in");
        }

        validationService.validateCheckIn(booking);

        // ✅ THÊM: Validate room assignment
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

        // ✅ THÊM: Validate room không đang được sử dụng
        Room assignedRoom = booking.getAssignedRoom();
        validateRoomAvailableForCheckIn(assignedRoom, booking);

        booking.setStatus(BookingStatus.CHECKED_IN);
        assignedRoom.setStatus("Đang sử dụng");
        roomRepository.save(assignedRoom);

        booking = bookingRepository.save(booking);

        // Notify admin about check-in
        adminNotificationService.notifyCheckIn(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    // ✅ SỬA validateRoomAvailableForCheckIn
    private void validateRoomAvailableForCheckIn(Room room, Booking currentBooking) {
        // Check 1: Room status
        if (!"Trống".equals(room.getStatus()) && !"Sẵn sàng".equals(room.getStatus())) {
            // ✅ SPECIAL CASE: Full payment booking flexibility
            if (BookingStatus.CONFIRMED.equals(currentBooking.getStatus())) {
                // Có thể negotiate với admin nếu là full payment
                log.warn("Full payment booking {} attempting check-in to occupied room {}",
                        currentBooking.getId(), room.getRoomNumber());
            }
            throw new RuntimeException("Phòng " + room.getRoomNumber() +
                    " đang có trạng thái '" + room.getStatus() + "'. " +
                    (BookingStatus.CONFIRMED.equals(currentBooking.getStatus()) ?
                            "Vui lòng liên hệ lễ tân để được hỗ trợ." :
                            "Không thể check-in."));
        }

        // Check 2: Không có booking khác đang active
        boolean hasActiveBooking = bookingRepository.hasActiveBookingForRoom(
                room.getId(),
                currentBooking.getCheckInDate(),
                currentBooking.getCheckOutDate(),
                currentBooking.getId()
        );

        if (hasActiveBooking) {
            if (BookingStatus.CONFIRMED.equals(currentBooking.getStatus())) {
                // Full payment → Admin cần resolve conflict
                throw new RuntimeException("Phòng " + room.getRoomNumber() +
                        " đã có booking khác. Vui lòng liên hệ manager để được hỗ trợ chuyển phòng.");
            } else {
                throw new RuntimeException("Phòng " + room.getRoomNumber() +
                        " đã có booking khác đang active, không thể check-in");
            }
        }

        // Check 3: Không có booking đang checked-in
        boolean hasCheckedInBooking = bookingRepository.hasCheckedInBookingForRoom(room.getId(), currentBooking.getId());

        if (hasCheckedInBooking) {
            throw new RuntimeException("Phòng " + room.getRoomNumber() +
                    " đang có khách ở, không thể check-in booking mới");
        }
    }

    @Override
    public BookingResponse checkOutBooking(Long bookingId) {
        User currentUser = userService.getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        boolean isAdmin = isUserAdmin(currentUser);
        if (!isAdmin) {
            throw new RuntimeException("Chỉ admin mới có thể thực hiện check-out");
        }

        validationService.validateCheckOut(booking);

        booking.setStatus(BookingStatus.COMPLETED);
        Room assignedRoom = booking.getAssignedRoom();
        if (assignedRoom != null) {
            assignedRoom.setStatus("Trống");
            roomRepository.save(assignedRoom);
            log.info("Updated room {} status to 'Trống' on check-out",
                    assignedRoom.getRoomNumber());
        }
        booking = bookingRepository.save(booking);

        return mappingService.mapToBookingResponse(booking);
    }

    // ========== SCHEDULED TASKS (UPDATED) ==========

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredTemporaryBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(TEMPORARY_BOOKING_EXPIRE_MINUTES);
        List<Booking> expiredBookings = bookingRepository.findExpiredTemporaryBookings(cutoffTime);

        expiredBookings.forEach(booking -> {
            boolean hasSuccessfulPayment = booking.getPayments().stream()
                    .anyMatch(Payment::isPaid);

            if (hasSuccessfulPayment) {
                // Determine final status based on payment type
                Payment paidPayment = booking.getPayments().stream()
                        .filter(Payment::isPaid)
                        .findFirst()
                        .orElse(null);

                if (paidPayment != null && paidPayment.isDeposit()) {
                    booking.setStatus(BookingStatus.PAID); // "Đã thanh toán" with remaining amount
                } else {
                    booking.setStatus(BookingStatus.CONFIRMED); // "Đã xác nhận" - fully paid
                }

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

    // ========== PRIVATE HELPER METHODS ==========

    private boolean isUserAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));
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

    private Room validateAndGetSpecificRoom(BookingRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Phòng không tồn tại với ID: " + request.getRoomId()));

        if (!room.getRoomType().getId().equals(request.getRoomTypeId())) {
            throw new RuntimeException("Phòng không thuộc loại phòng đã chọn");
        }

        if (!availabilityService.isSpecificRoomAvailable(request.getRoomId(),
                request.getCheckInDate(), request.getCheckOutDate())) {
            throw new RuntimeException("Phòng " + room.getRoomNumber() + " không khả dụng trong thời gian đã chọn");
        }

        return room;
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

    // ✅ Handle refund processing using PaymentService
    private void handleCancellationRefund(Booking booking) {
        List<Payment> payments = paymentRepository.findByBookingId(booking.getId());

        for (Payment payment : payments) {
            if (payment.isPaid() && !payment.isRefund()) {
                BigDecimal refundAmount = calculateRefundAmount(booking, payment);

                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // Create refund using PaymentService
                    paymentService.createRefundPayment(
                            booking.getId(),
                            refundAmount,
                            "Hoàn tiền do hủy booking #" + booking.getId()
                    );

                    log.info("Created refund {} VND for cancelled booking {}",
                            refundAmount, booking.getId());
                }
            }
        }
    }

    private BigDecimal calculateRefundAmount(Booking booking, Payment payment) {
        // Simple refund policy:
        // - If cancelled more than 24h before checkin: 100% refund
        // - If cancelled less than 24h: 50% refund for deposit, 80% for full payment

        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = java.time.temporal.ChronoUnit.DAYS.between(now, booking.getCheckInDate());

        if (daysUntilCheckIn >= 1) {
            return payment.getAmount(); // Full refund
        } else {
            if (payment.isDeposit()) {
                return payment.getAmount().multiply(BigDecimal.valueOf(0.5)); // 50% refund for deposit
            } else {
                return payment.getAmount().multiply(BigDecimal.valueOf(0.8)); // 80% refund for full payment
            }
        }
    }

    // ✅ THÊM VÀO BookingServiceImpl.java

    // ✅ SỬA BookingServiceImpl - Chỉ auto-expire DEPOSIT booking
    @Scheduled(fixedRate = 1800000) // 30 phút
    @Transactional
    public void autoExpireDepositBookings() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // ✅ CHỈ tìm DEPOSIT booking (PAID status + có remaining amount)
        List<Booking> expiredBookings = bookingRepository
                .findDepositOnlyBookingsPassedCheckIn(yesterday);

        for (Booking booking : expiredBookings) {
            log.info("Auto-expiring deposit booking: {}", booking.getId());

            booking.setStatus(BookingStatus.CANCELLED);

            if (booking.getAssignedRoom() != null) {
                Room room = booking.getAssignedRoom();
                room.setStatus("Trống");
                roomRepository.save(room);
            }
        }

        if (!expiredBookings.isEmpty()) {
            bookingRepository.saveAll(expiredBookings);
            log.info("Auto-expired {} deposit bookings", expiredBookings.size());
        }
    }

// ✅ XÓA hoặc comment autoExpireFullPaymentBookings() method
// Không cần auto-expire full payment bookings


}