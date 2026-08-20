package com.programandoenjava.airline.notification.infrastructure.adapter.in.events.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * passengerId was added to this contract for EPIC-05. Without it a payment
 * message could not be turned into anything addressed to anyone.
 */
public record PaymentSucceededEvent(UUID eventId,
                                    UUID bookingId,
                                    UUID passengerId,
                                    BigDecimal amount,
                                    String currency) {
}
