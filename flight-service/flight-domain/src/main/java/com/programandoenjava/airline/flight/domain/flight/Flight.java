package com.programandoenjava.airline.flight.domain;

import java.time.Instant;

public record Flight(FlightId id,
                     FlightNumber number,
                     AirportCode origin,
                     AirportCode destination,
                     FlightSchedule schedule,
                     SeatInventory seats,
                     Money price) {

    public Flight {
        if (id == null || number == null || schedule == null
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

    /**
     * A newly scheduled flight, with every seat still for sale.
     */
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

    /**
     * Returns this flight with the given seats blocked.
     */
    private Flight blockSeats(int count) {
        SeatInventory blocked = seats.block(count);
        return new Flight(id, number, origin, destination, schedule, blocked, price);
    }

    public Flight releaseSeats(int count) {
        SeatInventory released = seats.release(count);
        return new Flight(id, number, origin, destination, schedule, released, price);
    }

    public BlockResult block(BookingId bookingId, SeatCount requested, Instant now) {
        if (!schedule.departsAfter(now)) {
            throw new DomainValidationException(
                    "Flight " + number.value() + " is no longer open for booking");
        }
        Flight reduced = blockSeats(requested.value());
        SeatBlock block = new SeatBlock(
                SeatBlockId.newId(), id, bookingId, requested, now);
        return new BlockResult(reduced, block);
    }
}
