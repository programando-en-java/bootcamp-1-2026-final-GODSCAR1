package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

import java.util.UUID;

public record PassengerId(UUID value) {

    public PassengerId {
        if (value == null) {
            throw new DomainValidationException("A passenger id is required");
        }
    }
}
