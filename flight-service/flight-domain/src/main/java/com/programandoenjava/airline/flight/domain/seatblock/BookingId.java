package com.programandoenjava.airline.flight.domain.seatblock;

import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;

import java.util.UUID;

public record BookingId(UUID value) {

    public BookingId {
        if (value == null) {
            throw new DomainValidationException("A booking id is required");
        }
    }
}