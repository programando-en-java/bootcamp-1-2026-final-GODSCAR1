package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

public record BoardingSequence(int value) {

    public static final int FIRST = 1;

    public BoardingSequence {
        if (value < FIRST) {
            throw new DomainValidationException(
                    "A boarding sequence starts at " + FIRST + ", was: " + value);
        }
    }
}
