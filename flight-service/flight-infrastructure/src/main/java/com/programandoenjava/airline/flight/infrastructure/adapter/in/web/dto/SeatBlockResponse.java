package com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto;

import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

import java.time.Instant;
import java.util.UUID;

public record SeatBlockResponse(
        UUID seatBlockId,
        UUID flightId,
        UUID bookingId,
        int seats,
        Instant blockedAt) {

    public static SeatBlockResponse from(final SeatBlock block) {
        return new SeatBlockResponse(
                block.id().value(),
                block.flightId().value(),
                block.bookingId().value(),
                block.seats().value(),
                block.blockedAt());
    }
}
