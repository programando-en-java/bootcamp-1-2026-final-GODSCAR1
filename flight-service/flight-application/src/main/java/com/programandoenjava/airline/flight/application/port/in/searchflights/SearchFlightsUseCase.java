package com.programandoenjava.airline.flight.application.port.in.searchflights;

import com.programandoenjava.airline.flight.application.port.shared.PageResult;
import com.programandoenjava.airline.flight.domain.flight.Flight;

public interface SearchFlightsUseCase {
    PageResult<Flight> search(SearchFlightsQuery query);
}
