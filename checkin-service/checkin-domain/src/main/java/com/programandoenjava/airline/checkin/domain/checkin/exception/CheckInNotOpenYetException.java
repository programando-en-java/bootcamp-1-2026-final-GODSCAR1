package com.programandoenjava.airline.checkin.domain.checkin.exception;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

public class CheckInNotOpenYetException extends DomainValidationException {

    public CheckInNotOpenYetException(final String message) {
        super(message);
    }
}
