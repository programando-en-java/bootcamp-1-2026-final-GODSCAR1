package com.programandoenjava.airline.notification.infrastructure.adapter.in.events.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingCreatedEvent(UUID eventId,
                                  UUID bookingId,
                                  UUID passengerId,
                                  int seats,
                                  BigDecimal total,
                                  String currency) {
}
