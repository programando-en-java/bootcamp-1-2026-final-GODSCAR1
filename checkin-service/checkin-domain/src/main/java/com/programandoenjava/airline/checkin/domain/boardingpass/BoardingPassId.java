package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

import java.util.UUID;

public record BoardingPassId(UUID value) {

    public BoardingPassId {
        if (value == null) {
            throw new DomainValidationException("A boarding pass id is required");
        }
    }

    public static BoardingPassId newId() {
        return new BoardingPassId(UUID.randomUUID());
    }
}
