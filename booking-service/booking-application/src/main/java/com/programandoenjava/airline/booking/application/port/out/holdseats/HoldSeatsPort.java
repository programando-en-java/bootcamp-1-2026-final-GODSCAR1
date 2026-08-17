package com.programandoenjava.airline.booking.application.port.out.holdseats;

public interface HoldSeatsPort {

    SeatsHeld hold(HoldSeatsCommand command);
}
