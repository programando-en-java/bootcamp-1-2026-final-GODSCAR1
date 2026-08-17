package com.programandoenjava.airline.flight.domain.flight.exception;

import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;

public class FlightDepartedException extends DomainValidationException {

    public FlightDepartedException(final String message) {
        super(message);
    }
}
