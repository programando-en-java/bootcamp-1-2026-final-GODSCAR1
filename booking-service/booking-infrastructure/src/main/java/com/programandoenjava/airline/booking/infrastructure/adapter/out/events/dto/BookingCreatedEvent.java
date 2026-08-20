package com.programandoenjava.airline.booking.infrastructure.adapter.out.events.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The contract other services read. Flat and made of nothing but primitives, so
 * a change to Booking cannot reach a consumer by accident (ADR-001).
 *
 * <p>The flight travels as an id and not as a number, because booking-service
 * does not know its number: it holds seats by id and never asks what the flight
 * is called. A consumer that needs the number has to ask flight-service.
 *
 * <p>eventId travels because delivery is at-least-once: it is what lets a
 * consumer recognise a message it has already acted on.
 */
public record BookingCreatedEvent(UUID eventId,
                                  UUID bookingId,
                                  UUID passengerId,
                                  UUID flightId,
                                  int seats,
                                  BigDecimal total,
                                  String currency,
                                  String status,
                                  Instant createdAt) {
}
