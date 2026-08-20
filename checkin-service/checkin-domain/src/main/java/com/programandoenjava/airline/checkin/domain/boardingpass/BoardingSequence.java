package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

/**
 * Where a passenger stands in the boarding order, which is what this pass
 * carries instead of a seat. Nothing here models a seat because nothing here
 * owns one: seat inventory belongs to flight-service.
 */
public record BoardingSequence(int value) {

    public static final int FIRST = 1;

    public BoardingSequence {
        if (value < FIRST) {
            throw new DomainValidationException(
                    "A boarding sequence starts at " + FIRST + ", was: " + value);
        }
    }
}
