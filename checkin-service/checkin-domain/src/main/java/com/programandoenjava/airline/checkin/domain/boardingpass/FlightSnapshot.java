package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

import java.time.Instant;

/**
 * What the flight said when the pass was printed, copied rather than looked up.
 * A boarding pass is a document: it records a moment, and reprinting it must
 * not depend on flight-service still answering.
 *
 * <p>The route is validated as present and no further. flight-service owns what
 * a well formed airport code is, and checking it twice would only mean two
 * places to change when it changes.
 */
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
