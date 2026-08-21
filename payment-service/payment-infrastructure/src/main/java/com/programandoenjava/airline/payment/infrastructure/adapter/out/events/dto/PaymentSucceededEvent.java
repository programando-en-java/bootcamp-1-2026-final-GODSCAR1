package com.programandoenjava.airline.payment.infrastructure.adapter.out.events.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(UUID eventId,
                                    UUID paymentId,
                                    UUID bookingId,
                                    UUID passengerId,
                                    BigDecimal amount,
                                    String currency,
                                    Instant processedAt) {
}
