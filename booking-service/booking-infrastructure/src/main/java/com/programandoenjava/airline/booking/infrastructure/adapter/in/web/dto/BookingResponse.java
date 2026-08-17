package com.programandoenjava.airline.booking.infrastructure.adapter.in.web.dto;

import com.programandoenjava.airline.booking.domain.booking.Booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(UUID bookingId,
                              UUID passengerId,
                              UUID flightId,
                              int seats,
                              BigDecimal pricePerSeat,
                              BigDecimal total,
                              String currency,
                              String status,
                              Instant createdAt) {

    public static BookingResponse from(final Booking booking) {
        String currency = booking.total().currency().getCurrencyCode();
        String status = booking.status().name();

        return new BookingResponse(
                booking.id().value(),
                booking.passengerId().value(),
                booking.flightId().value(),
                booking.seats().value(),
                booking.pricePerSeat().amount(),
                booking.total().amount(),
                currency,
                status,
                booking.createdAt());
    }
}
