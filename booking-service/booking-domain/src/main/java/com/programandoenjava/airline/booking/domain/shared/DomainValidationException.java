package com.programandoenjava.airline.booking.domain.shared;

public class DomainValidationException extends RuntimeException {

    public DomainValidationException(final String message) {
        super(message);
    }
}
