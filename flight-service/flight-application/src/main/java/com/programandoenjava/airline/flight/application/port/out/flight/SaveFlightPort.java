package com.programandoenjava.airline.flight.application.port.out.flight;

import com.programandoenjava.airline.flight.domain.flight.Flight;

public interface SaveFlightPort {

    void save(Flight flight);
}
