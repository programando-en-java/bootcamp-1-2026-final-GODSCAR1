package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

import java.time.Instant;

public record FlightSnapshot(FlightId flightId,
                             String flightNumber,
                             String origin,
                             String destination,
                             Instant departure) {

    public FlightSnapshot {
        if (flightId == null) {
            throw new DomainValidationException("A flight id is required");
        }
        if (isBlank(flightNumber)) {
            throw new DomainValidationException("A flight number is required");
        }
        if (isBlank(origin) || isBlank(destination)) {
            throw new DomainValidationException("An origin and a destination are required");
        }
        if (departure == null) {
            throw new DomainValidationException("A departure time is required");
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
