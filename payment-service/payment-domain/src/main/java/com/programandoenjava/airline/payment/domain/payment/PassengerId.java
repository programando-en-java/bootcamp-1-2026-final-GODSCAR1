package com.programandoenjava.airline.payment.domain.payment;

import com.programandoenjava.airline.payment.domain.shared.DomainValidationException;

import java.util.UUID;

public record PassengerId(UUID value) {

    public PassengerId {
        if (value == null) {
            throw new DomainValidationException("A passenger id is required");
        }
    }
}
