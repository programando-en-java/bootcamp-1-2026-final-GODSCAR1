package com.programandoenjava.airline.checkin.infrastructure.adapter.out.events.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckInCompletedEvent(UUID eventId,
                                    UUID boardingPassId,
                                    UUID bookingId,
                                    UUID passengerId,
                                    UUID flightId,
                                    String flightNumber,
                                    String origin,
                                    String destination,
                                    Instant departureTime,
                                    int boardingSequence,
                                    Instant issuedAt) {
}
