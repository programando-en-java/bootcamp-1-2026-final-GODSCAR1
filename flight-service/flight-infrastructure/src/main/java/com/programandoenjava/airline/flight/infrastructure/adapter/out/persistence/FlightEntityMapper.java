package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence;

import com.programandoenjava.airline.flight.domain.AirportCode;
import com.programandoenjava.airline.flight.domain.Flight;
import com.programandoenjava.airline.flight.domain.FlightId;
import com.programandoenjava.airline.flight.domain.FlightNumber;
import com.programandoenjava.airline.flight.domain.FlightSchedule;
import com.programandoenjava.airline.flight.domain.Money;
import com.programandoenjava.airline.flight.domain.SeatInventory;

import java.util.Currency;

/**
 * Turns persistence rows into domain aggregates.
 */
final class FlightEntityMapper {

    private FlightEntityMapper() {
    }

    static Flight toDomain(FlightEntity entity) {
        return new Flight(
                new FlightId(entity.getId()),
                new FlightNumber(entity.getFlightNumber()),
                new AirportCode(entity.getOrigin()),
                new AirportCode(entity.getDestination()),
                new FlightSchedule(entity.getDepartureTime(), entity.getArrivalTime()),
                new SeatInventory(entity.getTotalSeats(), entity.getAvailableSeats()),
                new Money(entity.getPriceAmount(),
                        Currency.getInstance(entity.getPriceCurrency())));
    }
}