package com.programandoenjava.airline.booking.infrastructure.adapter.in.events.dto;

import java.util.UUID;

public record PaymentFailedEvent(UUID eventId, UUID bookingId) {
}
