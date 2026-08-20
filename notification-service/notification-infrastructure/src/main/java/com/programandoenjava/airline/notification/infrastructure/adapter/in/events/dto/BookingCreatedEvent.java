package com.programandoenjava.airline.notification.infrastructure.adapter.in.events.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Only the fields a notification is written from. booking-service sends the
 * flight id, the status and when it happened as well; ignoring them is what
 * lets it add a field without breaking anything here.
 */
public record BookingCreatedEvent(UUID eventId,
                                  UUID bookingId,
                                  UUID passengerId,
                                  int seats,
                                  BigDecimal total,
                                  String currency) {
}
