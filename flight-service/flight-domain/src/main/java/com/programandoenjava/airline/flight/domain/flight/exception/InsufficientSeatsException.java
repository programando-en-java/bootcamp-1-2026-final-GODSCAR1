package com.programandoenjava.airline.flight.domain.flight.exception;

import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;

public class InsufficientSeatsException extends DomainValidationException {

    public InsufficientSeatsException(final String message) {
        super(message);
    }
}
