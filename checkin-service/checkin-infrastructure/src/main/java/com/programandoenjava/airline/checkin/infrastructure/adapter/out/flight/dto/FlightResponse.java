package com.programandoenjava.airline.checkin.infrastructure.adapter.out.flight.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Only what a boarding pass is printed from. flight-service also sends the
 * fare, the arrival and the seats left, and none of those belong on a pass.
 */
public record FlightResponse(UUID id,
                             String flightNumber,
                             String origin,
                             String destination,
                             Instant departureTime) {
}
