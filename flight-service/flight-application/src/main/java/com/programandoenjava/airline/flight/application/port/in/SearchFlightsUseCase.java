package com.programandoenjava.airline.flight.application.port.in;

import com.programandoenjava.airline.flight.domain.Flight;

public interface SearchFlightsUseCase {
    PageResult<Flight> search(SearchFlightsQuery query);
}
