package com.programandoenjava.airline.booking.application.port.out.holdseats.exception;

public class FlightNotBookableException extends RuntimeException {

    public FlightNotBookableException(final String message) {
        super(message);
    }
}
