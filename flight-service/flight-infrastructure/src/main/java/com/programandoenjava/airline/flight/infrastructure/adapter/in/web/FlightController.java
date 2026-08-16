package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.application.port.in.PageQuery;
import com.programandoenjava.airline.flight.application.port.in.PageResult;
import com.programandoenjava.airline.flight.application.port.in.SearchFlightsQuery;
import com.programandoenjava.airline.flight.application.port.in.SearchFlightsUseCase;
import com.programandoenjava.airline.flight.domain.Flight;
import com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto.FlightResponse;
import com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto.SearchFlightsRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flights")
class FlightController {

    private final SearchFlightsUseCase searchFlightsUseCase;

    FlightController(SearchFlightsUseCase searchFlightsUseCase) {
        this.searchFlightsUseCase = searchFlightsUseCase;
    }

    @GetMapping
    PageResult<FlightResponse> search(
            @Valid SearchFlightsRequest request,
            @PageableDefault(size = PageQuery.MAX_SIZE) Pageable pageable) {

        SearchFlightsQuery query = SearchFlightsRequestMapper.toQuery(request, pageable);

        PageResult<Flight> flights = searchFlightsUseCase.search(query);

        return flights.map(FlightResponse::from);
    }
}