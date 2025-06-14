package com.example.demo.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentSuccessEvent extends ApplicationEvent {
    private final Long bookingId;

    public PaymentSuccessEvent(Object source, Long bookingId) {
        super(source);
        this.bookingId = bookingId;
    }

    public PaymentSuccessEvent(Long bookingId) {
        super(bookingId);
        this.bookingId = bookingId;
    }
}