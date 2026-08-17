package com.programandoenjava.airline.flight.application.port.out.searchflights;

import com.programandoenjava.airline.flight.application.port.shared.PageResult;
import com.programandoenjava.airline.flight.domain.flight.Flight;

public interface LoadFlightsPort {
    PageResult<Flight> search(FlightSearchCriteria criteria);
}
