package com.programandoenjava.airline.flight.domain.flight;

import com.programandoenjava.airline.flight.domain.seatblock.*;
import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import com.programandoenjava.airline.flight.domain.flight.exception.FlightDepartedException;
import com.programandoenjava.airline.flight.domain.shared.Money;

import java.time.Instant;

public record Flight(FlightId id,
                     FlightNumber flightNumber,
                     AirportCode origin,
                     AirportCode destination,
                     FlightSchedule schedule,
                     SeatInventory seats,
                     Money price) {

    public Flight {
        if (id == null || flightNumber == null || schedule == null
                || seats == null || price == null) {
            throw new DomainValidationException("Flight is missing required attributes");
        }
        if (origin == null || destination == null) {
            throw new DomainValidationException("Origin and destination are required");
        }
        if (origin.equals(destination)) {
            throw new DomainValidationException(
                    "Origin and destination must differ, both were: " + origin);
        }
    }

    public static Flight schedule(FlightNumber number,
                                  AirportCode origin,
                                  AirportCode destination,
                                  FlightSchedule schedule,
                                  int capacity,
                                  Money price) {
        return new Flight(FlightId.newId(), number, origin, destination,
                schedule, SeatInventory.empty(capacity), price);
    }

    public boolean isBookable(Instant now) {
        return schedule.departsAfter(now) && seats.hasAvailability();
    }

    private Flight blockSeats(int count) {
        SeatInventory blocked = seats.block(count);
        return new Flight(id, flightNumber, origin, destination, schedule, blocked, price);
    }

    public Flight releaseSeats(int count) {
        SeatInventory released = seats.release(count);
        return new Flight(id, flightNumber, origin, destination, schedule, released, price);
    }

    @SuppressWarnings("LocalCanBeFinal")
    public SeatsBlocked block(BookingId bookingId, SeatCount requested, @SuppressWarnings("LocalCanBeFinal") Instant now) {
        if (!schedule.departsAfter(now)) {
            throw new FlightDepartedException(
                    "Flight " + flightNumber.value() + " is no longer open for booking");
        }
        Flight reduced = blockSeats(requested.value());
        SeatBlock block = new SeatBlock(
                SeatBlockId.newId(), id, bookingId, requested, now);
        return new SeatsBlocked(reduced, block);
    }
}
