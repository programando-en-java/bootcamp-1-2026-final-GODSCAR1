package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.flight.FlightNumber;
import com.programandoenjava.airline.flight.domain.flight.FlightSchedule;
import com.programandoenjava.airline.flight.domain.shared.Money;
import com.programandoenjava.airline.flight.domain.flight.SeatInventory;

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

    static FlightEntity toEntity(Flight flight) {
        return new FlightEntity(
                flight.id().value(),
                flight.flightNumber().value(),
                flight.origin().value(),
                flight.destination().value(),
                flight.schedule().departure(),
                flight.schedule().arrival(),
                flight.seats().total(),
                flight.seats().available(),
                flight.price().amount(),
                flight.price().currency().getCurrencyCode());
    }
}