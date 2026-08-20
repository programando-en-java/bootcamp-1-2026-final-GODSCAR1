package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

import java.util.UUID;

public record FlightId(UUID value) {

    public FlightId {
        if (value == null) {
            throw new DomainValidationException("A flight id is required");
        }
    }
}
