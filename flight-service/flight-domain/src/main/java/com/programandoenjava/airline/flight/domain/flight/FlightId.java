package com.programandoenjava.airline.flight.domain.flight;

import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;

import java.util.UUID;

public record FlightId(UUID value) {

    public FlightId {
        if (value == null) {
            throw new DomainValidationException("Flight id is required");
        }
    }

    public static FlightId newId() {
        return new FlightId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}