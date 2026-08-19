package com.programandoenjava.airline.booking.infrastructure.adapter.in.events.dto;

import java.util.UUID;

/**
 * Only the two fields this service acts on. payment-service sends more; ignoring
 * the rest is what lets it add a field without breaking anything here.
 *
 * <p>Declared again rather than shared with payment-service (ADR-003), which
 * means the two can drift and nothing would say so.
 */
public record PaymentSucceededEvent(UUID eventId, UUID bookingId) {
}
