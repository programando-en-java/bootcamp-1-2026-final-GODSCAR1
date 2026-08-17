package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

final class FlightSpecifications {

    private FlightSpecifications() {
    }

    static Specification<FlightEntity> hasAvailableSeats() {
        return (root, query, builder) ->
                builder.greaterThan(root.get(FlightEntity_.availableSeats), 0);
    }

    static Specification<FlightEntity> departingAfter(Instant instant) {
        return (root, query, builder) ->
                builder.greaterThan(root.get(FlightEntity_.departureTime), instant);
    }

    static Specification<FlightEntity> departingBefore(Instant instant) {
        return (root, query, builder) ->
                builder.lessThan(root.get(FlightEntity_.departureTime), instant);
    }

    static Specification<FlightEntity> withOrigin(AirportCode origin) {
        return (root, query, builder) ->
                builder.equal(root.get(FlightEntity_.origin), origin.value());
    }

    static Specification<FlightEntity> withDestination(AirportCode destination) {
        return (root, query, builder) ->
                builder.equal(root.get(FlightEntity_.destination), destination.value());
    }
}
