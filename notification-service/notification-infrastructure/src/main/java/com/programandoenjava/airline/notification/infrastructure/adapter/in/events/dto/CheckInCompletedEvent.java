package com.programandoenjava.airline.notification.infrastructure.adapter.in.events.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckInCompletedEvent(UUID eventId,
                                    UUID bookingId,
                                    UUID passengerId,
                                    String flightNumber,
                                    String origin,
                                    String destination,
                                    Instant departureTime,
                                    int boardingSequence) {
}
