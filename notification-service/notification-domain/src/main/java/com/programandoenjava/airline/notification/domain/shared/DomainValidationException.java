package com.programandoenjava.airline.notification.domain.shared;

public class DomainValidationException extends RuntimeException {

    public DomainValidationException(final String message) {
        super(message);
    }
}
