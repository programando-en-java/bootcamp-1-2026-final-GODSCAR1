package com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto;

import com.programandoenjava.airline.flight.application.port.in.blockseats.SeatsHeld;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Seats held, as the caller sees them.
 *
 * <p>Carries the block's own id because releasing it later will need it, and
 * booking-service has nowhere else to get it from.
 *
 * <p>Carries the fare because the caller has to charge for what it reserved. A
 * price fetched separately afterwards could have moved between the two calls,
 * and the passenger would be billed for something other than what they booked.
 * The total is left to the caller: seats times fare is arithmetic, and sending
 * it as well would be a second number that could disagree with the first.
 */
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
