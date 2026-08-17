package com.programandoenjava.airline.flight.application.port.out;

import com.programandoenjava.airline.flight.application.port.in.shared.PageResult;
import com.programandoenjava.airline.flight.domain.flight.Flight;

public interface LoadFlightsPort {
    PageResult<Flight> search(FlightSearchCriteria criteria);
}
