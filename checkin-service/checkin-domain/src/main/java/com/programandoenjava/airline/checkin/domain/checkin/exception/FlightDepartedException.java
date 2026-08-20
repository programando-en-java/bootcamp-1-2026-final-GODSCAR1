package com.programandoenjava.airline.checkin.domain.checkin.exception;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

public class FlightDepartedException extends DomainValidationException {

    public FlightDepartedException(final String message) {
        super(message);
    }
}
