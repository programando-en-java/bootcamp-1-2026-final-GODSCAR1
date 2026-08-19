package com.programandoenjava.airline.checkin.domain.checkin.exception;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

public class CheckInClosedException extends DomainValidationException {

    public CheckInClosedException(final String message) {
        super(message);
    }
}
