package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingResponse(UUID bookingId,
                              UUID passengerId,
                              BigDecimal total,
                              String currency,
                              String status) {
}
