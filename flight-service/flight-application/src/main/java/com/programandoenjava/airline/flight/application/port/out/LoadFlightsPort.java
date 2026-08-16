package com.programandoenjava.airline.flight.application.port.out;

import com.programandoenjava.airline.flight.application.port.in.PageResult;
import com.programandoenjava.airline.flight.domain.Flight;

public interface LoadFlightsPort {
    PageResult<Flight> search(FlightSearchCriteria criteria);
}
