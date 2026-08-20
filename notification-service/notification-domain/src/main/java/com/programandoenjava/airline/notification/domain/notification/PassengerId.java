package com.programandoenjava.airline.notification.domain.notification;

import com.programandoenjava.airline.notification.domain.shared.DomainValidationException;

import java.util.UUID;

public record PassengerId(UUID value) {

    public PassengerId {
        if (value == null) {
            throw new DomainValidationException("A passenger id is required");
        }
    }
}
