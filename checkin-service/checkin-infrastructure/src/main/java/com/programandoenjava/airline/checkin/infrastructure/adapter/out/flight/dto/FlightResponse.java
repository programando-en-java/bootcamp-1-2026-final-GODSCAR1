package com.programandoenjava.airline.checkin.infrastructure.adapter.out.flight.dto;

import java.time.Instant;
import java.util.UUID;

public record FlightResponse(UUID id,
                             String flightNumber,
                             String origin,
                             String destination,
                             Instant departureTime) {
}
