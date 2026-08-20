package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Only the fields payment-service reads. booking-service sends more; the rest is
 * ignored on purpose, so a field added there does not break anything here.
 */
public record BookingResponse(UUID bookingId,
                              UUID passengerId,
                              BigDecimal total,
                              String currency,
                              String status) {
}
