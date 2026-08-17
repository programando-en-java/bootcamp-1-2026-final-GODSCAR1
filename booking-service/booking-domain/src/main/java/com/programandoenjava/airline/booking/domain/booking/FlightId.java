package com.programandoenjava.airline.booking.domain.booking;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;

import java.util.UUID;

public record FlightId(UUID value) {

    public FlightId {
        if (value == null) {
            throw new DomainValidationException("A flight id is required");
        }
    }
}
