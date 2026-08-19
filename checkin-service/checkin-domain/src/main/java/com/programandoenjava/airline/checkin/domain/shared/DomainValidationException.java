package com.programandoenjava.airline.checkin.domain.shared;

public class DomainValidationException extends RuntimeException {

    public DomainValidationException(final String message) {
        super(message);
    }
}
