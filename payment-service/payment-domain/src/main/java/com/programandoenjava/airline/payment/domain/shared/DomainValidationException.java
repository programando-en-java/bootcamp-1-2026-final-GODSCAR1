package com.programandoenjava.airline.payment.domain.shared;

public class DomainValidationException extends RuntimeException {

    public DomainValidationException(final String message) {
        super(message);
    }
}
