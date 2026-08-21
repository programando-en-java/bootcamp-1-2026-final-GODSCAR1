package com.programandoenjava.airline.notification.infrastructure.adapter.in.events.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSucceededEvent(UUID eventId,
                                    UUID bookingId,
                                    UUID passengerId,
                                    BigDecimal amount,
                                    String currency) {
}
