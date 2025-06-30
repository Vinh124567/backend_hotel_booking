package com.example.demo.event;

import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Room;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.service.notification.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final AdminNotificationService adminNotificationService;

    @EventListener
    @Async
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        try {
            Long bookingId = event.getBookingId();
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking không tồn tại với ID: " + bookingId));

            // ✅ THÊM: Get payment để xác định loại
            List<Payment> payments = paymentRepository.findByBookingId(bookingId);
            Payment latestPayment = payments.stream()
                    .filter(Payment::isPaid)
                    .max(Comparator.comparing(Payment::getPaymentDate))
                    .orElse(null);

            if (latestPayment == null) {
                log.warn("No paid payment found for booking {}", bookingId);
                return;
            }

            log.info("🎉 Processing payment success: bookingId={}, paymentType={}, amount={}",
                    bookingId, latestPayment.getPaymentType(), latestPayment.getAmount());

            // ✅ THÊM: Handle theo payment type
            handlePaymentByType(booking, latestPayment);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý payment success cho booking {}", event.getBookingId(), e);
        }
    }

    private void handlePaymentByType(Booking booking, Payment payment) {
        Payment.PaymentType paymentType = payment.getPaymentType();

        switch (paymentType) {
            case COC_TRUOC:
                handleDepositPaymentSuccess(booking, payment);
                break;

            case THANH_TOAN_CON_LAI:
                handleRemainingPaymentSuccess(booking, payment);
                break;

            case THANH_TOAN_DAY_DU:
                handleFullPaymentSuccess(booking, payment);
                break;

            default:
                log.warn("Unknown payment type: {} for booking {}", paymentType, booking.getId());
                handleFullPaymentSuccess(booking, payment); // Fallback
        }
    }

    private void handleDepositPaymentSuccess(Booking booking, Payment payment) {
        log.info("💰 Processing deposit payment success for booking {}", booking.getId());

        // Update booking status to "Đã thanh toán" với remaining amount
        booking.setStatus(BookingStatus.PAID);

        // Calculate remaining amount nếu chưa có
        if (booking.getRemainingAmount() == null || booking.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal remainingAmount = booking.getTotalPrice().subtract(payment.getAmount());
            booking.setRemainingAmount(remainingAmount);
        }

        bookingRepository.save(booking);
        adminNotificationService.notifyDepositPayment(booking, payment);
        log.info("✅ Deposit payment processed: remaining amount = {}", booking.getRemainingAmount());
    }

    // ✅ THÊM: Handle remaining payment success
    private void handleRemainingPaymentSuccess(Booking booking, Payment payment) {
        log.info("💳 Processing remaining payment success for booking {}", booking.getId());

        // Update booking to confirmed (fully paid)
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setRemainingAmount(BigDecimal.ZERO);

        // Auto-assign room if needed
        if (booking.getAssignedRoom() == null) {
            Room availableRoom = findAvailableRoom(booking);
            if (availableRoom != null) {
                booking.setAssignedRoom(availableRoom);
                availableRoom.setStatus("Đã đặt");
                roomRepository.save(availableRoom);
                log.info("Auto-assigned room {} to booking {}", availableRoom.getRoomNumber(), booking.getId());
            }
        }

        bookingRepository.save(booking);
        adminNotificationService.notifyFullPayment(booking, payment);

        log.info("✅ Remaining payment processed: booking confirmed");
    }

    // ✅ THÊM: Handle full payment success
    private void handleFullPaymentSuccess(Booking booking, Payment payment) {
        log.info("💯 Processing full payment success for booking {}", booking.getId());

        // Update booking to confirmed
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setDepositAmount(BigDecimal.ZERO);
        booking.setRemainingAmount(BigDecimal.ZERO);

        // Auto-assign room if needed
        if (booking.getAssignedRoom() == null) {
            Room availableRoom = findAvailableRoom(booking);
            if (availableRoom != null) {
                booking.setAssignedRoom(availableRoom);
                availableRoom.setStatus("Đã đặt");
                roomRepository.save(availableRoom);
                log.info("Auto-assigned room {} to booking {}", availableRoom.getRoomNumber(), booking.getId());
            }
        }

        bookingRepository.save(booking);
        adminNotificationService.notifyFullPayment(booking, payment);
        log.info("✅ Full payment processed: booking confirmed");
    }

    // Helper method
    private Room findAvailableRoom(Booking booking) {
        List<Room> availableRooms = roomRepository.findAvailableRoomsByTypeAndDates(
                booking.getRoomType().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate());
        return availableRooms.isEmpty() ? null : availableRooms.get(0);
    }

}