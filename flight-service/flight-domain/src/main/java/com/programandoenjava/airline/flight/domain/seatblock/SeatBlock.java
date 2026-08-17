package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.flight.FlightId;

import java.time.Instant;

public record SeatBlock(
        SeatBlockId id,
        FlightId flightId,
        BookingId bookingId,
        SeatCount seats,
        Instant blockedAt) {

    public SeatBlock {
        if (id == null) {
            throw new DomainValidationException("A seat block id is required");
        }
        if (flightId == null) {
            throw new DomainValidationException("A seat block must belong to a flight");
        }
        if (bookingId == null) {
            throw new DomainValidationException("A seat block must belong to a booking");
        }
        if (seats == null) {
            throw new DomainValidationException("A seat block must hold a number of seats");
        }
        if (blockedAt == null) {
            throw new DomainValidationException("A seat block must record when it was taken");
        }
    }
}