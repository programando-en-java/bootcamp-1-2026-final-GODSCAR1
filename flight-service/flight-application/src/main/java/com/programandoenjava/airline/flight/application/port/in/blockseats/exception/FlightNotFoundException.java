package com.programandoenjava.airline.flight.application.port.in.blockseats;

public class FlightNotFoundException extends RuntimeException {
    public FlightNotFoundException(String message) {
        super(message);
    }
}
