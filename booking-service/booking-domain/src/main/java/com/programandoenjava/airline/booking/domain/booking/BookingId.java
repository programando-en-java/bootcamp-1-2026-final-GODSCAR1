package com.programandoenjava.airline.booking.domain.booking;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;

import java.util.UUID;

public record BookingId(UUID value) {

    public BookingId {
        if (value == null) {
            throw new DomainValidationException("A booking id is required");
        }
    }

    public static BookingId newId() {
        UUID generated = UUID.randomUUID();

        return new BookingId(generated);
    }
}
