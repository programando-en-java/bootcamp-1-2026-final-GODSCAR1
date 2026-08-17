package com.programandoenjava.airline.flight.domain.shared;

public class DomainValidationException extends RuntimeException {

    public DomainValidationException(final String message) {
        super(message);
    }
}
