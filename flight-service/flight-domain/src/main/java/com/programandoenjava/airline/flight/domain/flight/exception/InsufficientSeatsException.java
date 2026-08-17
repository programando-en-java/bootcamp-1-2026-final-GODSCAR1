package com.programandoenjava.airline.flight.domain.shared;

public class InsufficientSeatsException extends DomainValidationException {

    public InsufficientSeatsException(String message) {
        super(message);
    }
}
