package com.programandoenjava.airline.booking.domain.booking;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;

import java.util.UUID;

public record PassengerId(UUID value) {

    public PassengerId {
        if (value == null) {
            throw new DomainValidationException("A passenger id is required");
        }
    }
}
