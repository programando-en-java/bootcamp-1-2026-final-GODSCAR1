package com.programandoenjava.airline.booking.infrastructure.adapter.out.events.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
