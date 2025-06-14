package com.example.demo.event;

import com.example.demo.dto.booking.BookingStatus;
import com.example.demo.entity.Booking;
import com.example.demo.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final BookingRepository bookingRepository;

    @EventListener
    @Async
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        try {
            Long bookingId = event.getBookingId();
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking không tồn tại với ID: " + bookingId));

            if (!BookingStatus.TEMPORARY.equals(booking.getStatus()) && !BookingStatus.PENDING.equals(booking.getStatus())) {
                log.warn("Booking {} không ở trạng thái hợp lệ để confirm: {}", bookingId, booking.getStatus());
                return;
            }

            long confirmedBookings = bookingRepository.countConfirmedOverlappingBookingsExcluding(
                    booking.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getId());

            if (confirmedBookings >= 5) {
                log.error("Phòng không còn khả dụng cho booking: {}", bookingId);
                return;
            }

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            log.info("Booking {} đã được confirm tự động sau khi thanh toán thành công", bookingId);

        } catch (Exception e) {
            log.error("Lỗi khi confirm booking {} sau payment success", event.getBookingId(), e);
        }
    }
}