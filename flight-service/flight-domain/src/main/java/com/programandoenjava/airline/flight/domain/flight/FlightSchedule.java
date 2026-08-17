package com.programandoenjava.airline.flight.domain.flight;

import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;

import java.time.Duration;
import java.time.Instant;

public record FlightSchedule(Instant departure, Instant arrival) {

    public FlightSchedule {
        if (departure == null || arrival == null) {
            throw new DomainValidationException("Departure and arrival are required");
        }
        if (!arrival.isAfter(departure)) {
            throw new DomainValidationException("Arrival must be after departure");
        }
    }

    public boolean departsAfter(final Instant instant) {
        return departure.isAfter(instant);
    }

    public Duration duration() {
        return Duration.between(departure, arrival);
    }
}
