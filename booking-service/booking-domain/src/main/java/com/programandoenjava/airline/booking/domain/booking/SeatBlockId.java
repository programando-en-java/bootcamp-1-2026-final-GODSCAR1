package com.programandoenjava.airline.booking.domain.booking;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;

import java.util.UUID;

public record SeatBlockId(UUID value) {

    public SeatBlockId {
        if (value == null) {
            throw new DomainValidationException("A seat block id is required");
        }
    }
}
