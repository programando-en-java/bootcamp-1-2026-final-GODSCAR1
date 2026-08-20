package com.programandoenjava.airline.checkin.infrastructure.adapter.out.events.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The contract other services read. Flat and made of nothing but primitives, so
 * a change to BoardingPass cannot reach a consumer by accident (ADR-001).
 *
 * <p>It carries what a notification would be written from, which is why the
 * flight is here in full rather than as an id nobody downstream could resolve
 * without another call.
 *
 * <p>eventId travels because delivery is at-least-once: it is what lets a
 * consumer recognise a message it has already acted on.
 */
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
