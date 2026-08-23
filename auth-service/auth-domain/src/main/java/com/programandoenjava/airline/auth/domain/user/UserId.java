package com.programandoenjava.airline.auth.domain.user;

import com.programandoenjava.airline.auth.domain.shared.DomainValidationException;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new DomainValidationException("A user id is required");
        }
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
