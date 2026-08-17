package com.programandoenjava.airline.flight.application.port.in;

import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsQuery;
import com.programandoenjava.airline.flight.domain.flight.Flight;

public interface SearchFlightsUseCase {
    PageResult<Flight> search(SearchFlightsQuery query);
}
