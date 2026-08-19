package com.programandoenjava.airline.payment.infrastructure.adapter.out.events.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(UUID eventId,
                                 UUID paymentId,
                                 UUID bookingId,
                                 BigDecimal amount,
                                 String currency,
                                 Instant processedAt) {
}
