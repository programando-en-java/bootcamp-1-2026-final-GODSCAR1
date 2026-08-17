package com.programandoenjava.airline.booking.application.port.out.holdseats.exception;

public class FlightNotFoundException extends RuntimeException {

    public FlightNotFoundException(final String message) {
        super(message);
    }
}
