package com.programandoenjava.airline.payment.infrastructure.adapter.out.events.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The contract other services read. Flat and made of nothing but primitives, so
 * that a change to Payment cannot reach a consumer by accident (ADR-001).
 *
 * <p>eventId is the outbox row's id, and it travels because delivery is
 * at-least-once: it is what lets a consumer recognise a message it has already
 * acted on.
 */
public record PaymentSucceededEvent(UUID eventId,
                                    UUID paymentId,
                                    UUID bookingId,
                                    BigDecimal amount,
                                    String currency,
                                    Instant processedAt) {
}
