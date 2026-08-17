package com.programandoenjava.airline.flight.domain.seatblock;

import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;

import java.util.UUID;

public record SeatBlockId(UUID value) {

    public SeatBlockId {
        if (value == null) {
            throw new DomainValidationException("A seat block id is required");
        }
    }

    public static SeatBlockId newId() {
        return new SeatBlockId(UUID.randomUUID());
    }
}
