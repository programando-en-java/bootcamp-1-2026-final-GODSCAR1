package com.programandoenjava.airline.booking.application.port.out.holdseats.exception;

public class SeatsUnavailableException extends RuntimeException {

    public SeatsUnavailableException(final String message) {
        super(message);
    }
}
