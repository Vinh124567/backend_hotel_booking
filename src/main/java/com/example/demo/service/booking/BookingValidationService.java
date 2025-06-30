package com.example.demo.service.booking;

import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import com.example.demo.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class BookingValidationService {
    private final BookingRepository bookingRepository;

    // ✅ BUSINESS RULES CONSTANTS
    private static final int SAME_DAY_CUTOFF_HOUR = 18; // 6PM
    private static final int CANCELLATION_HOURS_BEFORE = 24;
    private static final int MODIFICATION_HOURS_BEFORE = 48;
    private static final int MAX_ACTIVE_BOOKINGS_PER_USER = 5;
    private static final int MAX_ADVANCE_BOOKING_DAYS = 365;
    private static final int CHECK_IN_GRACE_DAYS = 1;
    private static final int CHECK_OUT_GRACE_DAYS = 1;

    // =========================================================================
    // BOOKING REQUEST VALIDATION
    // =========================================================================

    /**
     * Validate booking request for creation
     */
    public void validateBookingRequest(BookingRequest request) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        validateGuests(request.getNumberOfGuests());
        validatePrice(request.getTotalPrice());
        validateAdvanceBooking(request.getCheckInDate());

        // Check booking conflicts
        if (request.getRoomTypeId() != null) {
            validateBookingConflict(request.getRoomTypeId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate(),
                    null);
        }
    }

    /**
     * Validate date range with same-day booking support
     */
    private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate.isAfter(checkOutDate)) {
            throw new RuntimeException("Ngày check-in phải trước ngày check-out");
        }

        if (checkInDate.equals(checkOutDate)) {
            throw new RuntimeException("Thời gian lưu trú tối thiểu là 1 đêm");
        }

        validateCheckInDate(checkInDate);
    }

    /**
     * Validate check-in date with same-day booking support
     */
    private void validateCheckInDate(LocalDate checkInDate) {
        LocalDate today = LocalDate.now();

        // Không cho phép đặt phòng ngày quá khứ
        if (checkInDate.isBefore(today)) {
            throw new RuntimeException("Ngày check-in không thể là quá khứ");
        }

        // Nếu đặt phòng cùng ngày, kiểm tra cut-off time
        if (checkInDate.equals(today)) {
            LocalTime now = LocalTime.now();
            if (now.getHour() >= SAME_DAY_CUTOFF_HOUR) {
                throw new RuntimeException("Đã quá 6PM, không thể đặt phòng cùng ngày. Vui lòng chọn từ ngày mai.");
            }
        }
    }

    /**
     * Validate number of guests
     */
    private void validateGuests(Integer numberOfGuests) {
        if (numberOfGuests == null || numberOfGuests <= 0) {
            throw new RuntimeException("Số lượng khách phải lớn hơn 0");
        }
    }

    /**
     * Validate total price
     */
    private void validatePrice(Double totalPrice) {
        if (totalPrice == null || totalPrice <= 0) {
            throw new RuntimeException("Tổng giá phải lớn hơn 0");
        }
    }

    /**
     * Validate advance booking limit
     */
    private void validateAdvanceBooking(LocalDate checkInDate) {
        long daysInAdvance = ChronoUnit.DAYS.between(LocalDate.now(), checkInDate);
        if (daysInAdvance > MAX_ADVANCE_BOOKING_DAYS) {
            throw new RuntimeException("Chỉ có thể đặt phòng tối đa " + MAX_ADVANCE_BOOKING_DAYS + " ngày trước");
        }
    }

    // =========================================================================
    // PERMISSION VALIDATION METHODS
    // =========================================================================

    /**
     * Validate admin operation permission
     */
    public void validateAdminOperation(User user, String operation) {
        if (!isAdmin(user)) {
            throw new SecurityException("Chỉ admin mới có thể thực hiện thao tác: " + operation);
        }
    }

    /**
     * Validate booking ownership for user operations
     */
    // ✅ SỬA HÀM CŨ - CHỈ THAY ĐỔI EXCEPTION TYPE
    public void validateBookingOwnership(Booking booking, User user) {
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền thao tác với đặt phòng này");
        }
    }

    /**
     * Validate view permission - User xem của mình, Admin xem tất cả
     */
    public void validateViewPermission(Booking booking, User user) {
        if (!isAdmin(user)) {
            validateBookingOwnership(booking, user);
        }
        // Admin có thể xem tất cả booking
    }

    /**
     * Validate user cancellation permission
     */
    public void validateUserCancellationPermission(Booking booking, User user) {
        // User chỉ có thể hủy booking của mình
        validateBookingOwnership(booking, user);

        // Check business rules for cancellation
        validateCancellation(booking);
    }

    /**
     * Validate admin cancellation permission
     */
    public void validateAdminCancellationPermission(Booking booking, User user) {
        // Admin có thể hủy bất kỳ booking nào
        validateAdminOperation(user, "hủy booking");

        // Check business rules for cancellation
        validateCancellation(booking);
    }

    /**
     * Validate modification permission - User sửa của mình, Admin sửa tất cả
     */
    public void validateModificationPermission(Booking booking, User user) {
        if (!isAdmin(user)) {
            validateBookingOwnership(booking, user);
        }

        // Check business rules for modification
        validateModification(booking);
    }

    /**
     * Validate check-in permission - Chỉ dành cho admin
     */
    // ✅ ADD to BookingValidationService
    public void validateCheckInPermission(Booking booking, User user) {
        // Admin có thể check-in bất kỳ booking nào
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));

        if (!isAdmin) {
            // User thông thường phải sở hữu booking để check-in
            validateBookingOwnership(booking, user);
        }

        // Always validate business rules
        validateCheckIn(booking);
    }

    /**
     * Validate check-out permission - Chỉ dành cho admin
     */
    public void validateCheckOutPermission(Booking booking, User user) {
        validateAdminOperation(user, "check-out");
        validateCheckOut(booking);
    }

    /**
     * Validate confirmation permission - Chỉ dành cho admin
     */
    public void validateConfirmationPermission(Booking booking, User user) {
        validateAdminOperation(user, "xác nhận booking");
        validateBookingForConfirmation(booking);
    }

    /**
     * Validate approval/rejection permission - Chỉ dành cho admin
     */
    public void validateApprovalPermission(Booking booking, User user) {
        validateAdminOperation(user, "duyệt/từ chối booking");
        validateBookingForConfirmation(booking);
    }

    // =========================================================================
    // BUSINESS RULES VALIDATION
    // =========================================================================

    /**
     * Validate booking for confirmation
     */
    public void validateBookingForConfirmation(Booking booking) {
        String status = booking.getStatus();
        if (!BookingStatus.canBeConfirmed(status)) {
            throw new RuntimeException("Chỉ có thể xác nhận booking ở trạng thái 'Tạm giữ chỗ' hoặc 'Chờ xác nhận'");
        }
    }

    /**
     * Validate cancellation business rules
     */
    public void validateCancellation(Booking booking) {
        String status = booking.getStatus();

        if (!BookingStatus.isCancellable(status)) {
            throw new RuntimeException(getCancellationErrorMessage(status));
        }

        // ✅ Can always cancel unpaid bookings
        if (BookingStatus.TEMPORARY.equals(status) ||
                BookingStatus.PENDING.equals(status)) {
            return;
        }

        // ✅ Check time limit for paid bookings
        if (BookingStatus.PAID.equals(status) ||           // ✅ THÊM
                BookingStatus.CONFIRMED.equals(status) ||
                BookingStatus.DEPOSIT_PAID.equals(status)) {
            validateCancellationDeadline(booking.getCheckInDate());
        }
    }

    /**
     * Get appropriate cancellation error message
     */
    private String getCancellationErrorMessage(String status) {
        switch (status) {
            case BookingStatus.CANCELLED:
                return "Đặt phòng đã bị hủy trước đó";
            case BookingStatus.COMPLETED:
                return "Không thể hủy - Đặt phòng đã hoàn thành";
            case BookingStatus.CHECKED_IN:
                return "Không thể hủy - Đã nhận phòng";
            case BookingStatus.NO_SHOW:
                return "Không thể hủy - Booking đã được đánh dấu 'Không đến'";
            default:
                return "Không thể hủy booking ở trạng thái hiện tại: " + status;
        }
    }

    /**
     * Validate cancellation deadline
     */
    private void validateCancellationDeadline(LocalDate checkInDate) {
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        if (daysUntilCheckIn < 1) {
            throw new RuntimeException("Không thể hủy - Quá thời hạn " + CANCELLATION_HOURS_BEFORE + " giờ trước check-in");
        }
    }

    /**
     * Validate check-in business rules
     */
    public void validateCheckIn(Booking booking) {
        String status = booking.getStatus();

        if (!canCheckIn(status)) {
            throw new RuntimeException("Chỉ có thể check-in đặt phòng ở trạng thái hợp lệ. Trạng thái hiện tại: " + status);
        }

        validateCheckInTiming(booking.getCheckInDate());
    }

    /**
     * Check if booking can be checked in
     */
    private boolean canCheckIn(String status) {
        return BookingStatus.TEMPORARY.equals(status) ||
                BookingStatus.PENDING.equals(status) ||
                BookingStatus.CONFIRMED.equals(status) || BookingStatus.PAID.equals(status);
    }

    /**
     * Validate check-in timing
     */
    private void validateCheckInTiming(LocalDate checkInDate) {
        LocalDate now = LocalDate.now();

        if (now.isBefore(checkInDate)) {
            throw new RuntimeException("Chưa đến ngày check-in");
        }

        if (ChronoUnit.DAYS.between(checkInDate, now) > CHECK_IN_GRACE_DAYS) {
            throw new RuntimeException("Đã quá thời gian check-in (quá " + CHECK_IN_GRACE_DAYS + " ngày)");
        }
    }

    /**
     * Validate check-out business rules
     */
    public void validateCheckOut(Booking booking) {
        String status = booking.getStatus();

        if (!BookingStatus.CHECKED_IN.equals(status)) {
            throw new RuntimeException("Chỉ có thể check-out sau khi đã check-in. Trạng thái hiện tại: " + status);
        }

        validateCheckOutTiming(booking.getCheckOutDate());
    }

    /**
     * Validate check-out timing
     */
    private void validateCheckOutTiming(LocalDate checkOutDate) {
        LocalDate now = LocalDate.now();

        if (now.isAfter(checkOutDate.plusDays(CHECK_OUT_GRACE_DAYS))) {
            throw new RuntimeException("Đã quá thời gian check-out (quá " + CHECK_OUT_GRACE_DAYS + " ngày)");
        }
    }

    /**
     * Validate modification business rules
     */
    public void validateModification(Booking booking) {
        String status = booking.getStatus();

        if (!canBeModified(status)) {
            throw new RuntimeException("Không thể sửa đổi đặt phòng ở trạng thái hiện tại: " + status);
        }

        validateModificationDeadline(booking.getCheckInDate());
    }

    /**
     * Check if booking can be modified
     */
    private boolean canBeModified(String status) {
        return BookingStatus.TEMPORARY.equals(status) ||
                BookingStatus.PENDING.equals(status) ||
                BookingStatus.CONFIRMED.equals(status);
    }

    /**
     * Validate modification deadline
     */
    private void validateModificationDeadline(LocalDate checkInDate) {
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        if (daysUntilCheckIn < 2) {
            throw new RuntimeException("Không thể sửa đổi - Phải trước " + MODIFICATION_HOURS_BEFORE + " giờ so với check-in");
        }
    }

    // =========================================================================
    // LIMITS AND CONFLICTS VALIDATION
    // =========================================================================

    /**
     * Validate user booking limits
     */
    public void validateUserBookingLimits(Long userId) {
        long activeCount = bookingRepository.countByUserIdAndStatusIn(
                userId,
                Arrays.asList(BookingStatus.TEMPORARY, BookingStatus.PENDING,
                        BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );

        if (activeCount >= MAX_ACTIVE_BOOKINGS_PER_USER) {
            throw new RuntimeException("Bạn đã có quá nhiều booking chưa hoàn thành (" +
                    activeCount + "/" + MAX_ACTIVE_BOOKINGS_PER_USER +
                    "). Vui lòng hoàn thành hoặc hủy bớt trước khi đặt thêm.");
        }
    }

    /**
     * Validate booking conflict for room type
     */
    public void validateBookingConflict(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate, Long excludeBookingId) {
        boolean hasConflict = bookingRepository.hasBookingConflict(roomTypeId, checkInDate, checkOutDate, excludeBookingId);
        if (hasConflict) {
            throw new RuntimeException("Phòng đã được đặt trong khoảng thời gian này");
        }
    }

    /**
     * Validate room conflict for specific room
     */
    public void validateRoomConflict(Long roomId, LocalDate checkInDate, LocalDate checkOutDate, Long excludeBookingId) {
        boolean hasConflict = bookingRepository.hasRoomConflict(roomId, checkInDate, checkOutDate, excludeBookingId);
        if (hasConflict) {
            throw new RuntimeException("Phòng cụ thể đã được đặt trong khoảng thời gian này");
        }
    }

    // =========================================================================
    // SPECIAL BUSINESS RULES
    // =========================================================================

    /**
     * Validate weekend booking requirements
     */
    public void validateWeekendBooking(LocalDate checkInDate, LocalDate checkOutDate) {
        if (isWeekend(checkInDate) || isWeekend(checkOutDate)) {
            long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (nights < 2) {
                throw new RuntimeException("Đặt phòng cuối tuần yêu cầu tối thiểu 2 đêm");
            }
        }
    }

    /**
     * Check if date is weekend
     */
    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 6; // Saturday = 6, Sunday = 7
    }

    /**
     * Validate admin override for status changes
     */
    public void validateAdminOverride(String currentStatus, String newStatus, User admin) {
        if (!isAdmin(admin)) {
            throw new SecurityException("Chỉ admin mới có thể thay đổi trạng thái booking");
        }

        if (!BookingStatus.isValidStatus(newStatus)) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + newStatus);
        }

        // Additional business rules for status transitions
        validateStatusTransition(currentStatus, newStatus);
    }

    /**
     * Validate status transition rules
     */
    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Define invalid transitions
        if (BookingStatus.COMPLETED.equals(currentStatus) && !BookingStatus.COMPLETED.equals(newStatus)) {
            throw new RuntimeException("Không thể thay đổi trạng thái booking đã hoàn thành");
        }

        if (BookingStatus.CANCELLED.equals(currentStatus) && !BookingStatus.CANCELLED.equals(newStatus)) {
            throw new RuntimeException("Không thể thay đổi trạng thái booking đã hủy");
        }

        if (BookingStatus.NO_SHOW.equals(currentStatus) && !BookingStatus.NO_SHOW.equals(newStatus)) {
            throw new RuntimeException("Không thể thay đổi trạng thái booking 'Không đến'");
        }
    }

    /**
     * Validate payment completion before status change
     */
    public void validatePaymentForStatusChange(Booking booking, String newStatus) {
        if (BookingStatus.CONFIRMED.equals(newStatus) || BookingStatus.CHECKED_IN.equals(newStatus)) {
            boolean hasSuccessfulPayment = bookingRepository.existsSuccessfulPaymentForBooking(booking.getId());
            if (!hasSuccessfulPayment) {
                throw new RuntimeException("Không thể thay đổi trạng thái - Chưa có thanh toán thành công");
            }
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    /**
     * Check if user is admin
     */
    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));
    }

    /**
     * Validate booking exists
     */
    public void validateBookingExists(Booking booking) {
        if (booking == null) {
            throw new RuntimeException("Booking không tồn tại");
        }
    }

    /**
     * Validate user exists
     */
    public void validateUserExists(User user) {
        if (user == null) {
            throw new SecurityException("User không tồn tại");
        }
    }

    /**
     * Get validation summary for booking
     */
    public String getValidationSummary(Booking booking, User user, String operation) {
        StringBuilder summary = new StringBuilder();
        summary.append("Validation for operation: ").append(operation).append("\n");
        summary.append("Booking ID: ").append(booking.getId()).append("\n");
        summary.append("Booking Status: ").append(booking.getStatus()).append("\n");
        summary.append("User: ").append(user.getUsername()).append("\n");
        summary.append("Is Admin: ").append(isAdmin(user)).append("\n");
        summary.append("Check-in Date: ").append(booking.getCheckInDate()).append("\n");
        summary.append("Check-out Date: ").append(booking.getCheckOutDate()).append("\n");
        return summary.toString();
    }
}