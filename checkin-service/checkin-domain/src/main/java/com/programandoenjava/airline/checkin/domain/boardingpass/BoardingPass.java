package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

import java.time.Instant;

public record BoardingPass(BoardingPassId id,
                           BookingId bookingId,
                           PassengerId passengerId,
                           FlightSnapshot flight,
                           BoardingSequence sequence,
                           Instant issuedAt) {

    public BoardingPass {
        if (id == null) {
            throw new DomainValidationException("A boarding pass id is required");
        }
        if (bookingId == null) {
            throw new DomainValidationException("A boarding pass must name a booking");
        }
        if (passengerId == null) {
            throw new DomainValidationException("A boarding pass must name a passenger");
        }
        if (flight == null) {
            throw new DomainValidationException("A boarding pass must name a flight");
        }
        if (sequence == null) {
            throw new DomainValidationException("A boarding pass must carry a boarding sequence");
        }
        if (issuedAt == null) {
            throw new DomainValidationException("A boarding pass must record when it was issued");
        }
    }

    public static BoardingPass issue(final BookingId bookingId,
                                     final PassengerId passengerId,
                                     final FlightSnapshot flight,
                                     final BoardingSequence sequence,
                                     final Instant now) {
        return new BoardingPass(BoardingPassId.newId(), bookingId, passengerId,
                flight, sequence, now);
    }
}
