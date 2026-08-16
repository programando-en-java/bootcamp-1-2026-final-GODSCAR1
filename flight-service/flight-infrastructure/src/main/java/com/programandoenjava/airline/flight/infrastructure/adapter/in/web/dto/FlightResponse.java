package com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto;

import com.programandoenjava.airline.flight.domain.Flight;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FlightResponse(UUID id,
                             String flightNumber,
                             String origin,
                             String destination,
                             Instant departureTime,
                             Instant arrivalTime,
                             int availableSeats,
                             BigDecimal price,
                             String currency) {

    public static FlightResponse from(Flight flight) {
        return new FlightResponse(
                flight.id().value(),
                flight.number().value(),
                flight.origin().value(),
                flight.destination().value(),
                flight.schedule().departure(),
                flight.schedule().arrival(),
                flight.seats().available(),
                flight.price().amount(),
                flight.price().currency().getCurrencyCode());
    }
}