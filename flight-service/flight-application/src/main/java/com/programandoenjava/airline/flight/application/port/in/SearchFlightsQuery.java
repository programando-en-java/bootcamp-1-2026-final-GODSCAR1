package com.programandoenjava.airline.flight.application.port.in;

import com.programandoenjava.airline.flight.domain.AirportCode;

import java.time.LocalDate;
import java.util.Objects;

public record SearchFlightsQuery(AirportCode origin,
                                 AirportCode destination,
                                 LocalDate departureDate,
                                 PageQuery page) {

    public SearchFlightsQuery {
        Objects.requireNonNull(page, "page is required");
    }

    public static SearchFlightsQuery unfiltered(PageQuery page) {
        return new SearchFlightsQuery(null, null, null, page);
    }
}