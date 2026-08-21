package com.programandoenjava.airline.flight.application.usecase;

import com.programandoenjava.airline.flight.application.port.in.readflight.ReadFlightUseCase;
import com.programandoenjava.airline.flight.application.port.out.flight.FindFlightPort;
import com.programandoenjava.airline.flight.application.port.shared.exception.FlightNotFoundException;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.flight.FlightId;

public class ReadFlightService implements ReadFlightUseCase {

    private final FindFlightPort findFlight;

    public ReadFlightService(final FindFlightPort findFlight) {
        this.findFlight = findFlight;
    }

    @Override
    public Flight byId(final FlightId id) {
        return findFlight.byId(id)
                .orElseThrow(() -> new FlightNotFoundException(id));
    }
}
