package com.programandoenjava.airline.notification.domain.notification;

import com.programandoenjava.airline.notification.domain.shared.DomainValidationException;

import java.util.UUID;

public record BookingId(UUID value) {

    public BookingId {
        if (value == null) {
            throw new DomainValidationException("A booking id is required");
        }
    }
}
