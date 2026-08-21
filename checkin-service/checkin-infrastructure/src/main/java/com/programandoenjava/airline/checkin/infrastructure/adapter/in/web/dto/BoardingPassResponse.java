package com.programandoenjava.airline.checkin.infrastructure.adapter.in.web.dto;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;

import java.time.Instant;
import java.util.UUID;

public record BoardingPassResponse(UUID boardingPassId,
                                   UUID bookingId,
                                   UUID passengerId,
                                   UUID flightId,
                                   String flightNumber,
                                   String origin,
                                   String destination,
                                   Instant departureTime,
                                   int boardingSequence,
                                   Instant issuedAt) {

    public static BoardingPassResponse from(final BoardingPass pass) {
        FlightSnapshot flight = pass.flight();

        return new BoardingPassResponse(
                pass.id().value(),
                pass.bookingId().value(),
                pass.passengerId().value(),
                flight.flightId().value(),
                flight.flightNumber(),
                flight.origin(),
                flight.destination(),
                flight.departure(),
                pass.sequence().value(),
                pass.issuedAt());
    }
}
