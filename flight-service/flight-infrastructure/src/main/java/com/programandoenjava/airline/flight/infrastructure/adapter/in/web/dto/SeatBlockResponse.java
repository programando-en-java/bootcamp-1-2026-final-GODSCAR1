package com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto;

import com.programandoenjava.airline.flight.application.port.in.blockseats.SeatsHeld;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SeatBlockResponse(
        UUID seatBlockId,
        UUID flightId,
        UUID bookingId,
        int seats,
        BigDecimal pricePerSeat,
        String currency,
        Instant blockedAt) {

    public static SeatBlockResponse from(final SeatsHeld held) {
        return new SeatBlockResponse(
                held.block().id().value(),
                held.block().flightId().value(),
                held.block().bookingId().value(),
                held.block().seats().value(),
                held.pricePerSeat().amount(),
                held.pricePerSeat().currency().getCurrencyCode(),
                held.block().blockedAt());
    }
}
