package com.programandoenjava.airline.flight.application.port.out;

import com.programandoenjava.airline.flight.application.port.in.PageQuery;
import com.programandoenjava.airline.flight.domain.AirportCode;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A fully resolved search, ready to be translated into a query.
 */
public record FlightSearchCriteria(AirportCode origin,
                                   AirportCode destination,
                                   Instant departingFrom,
                                   Instant departingBefore,
                                   int page,
                                   int size,
                                   List<PageQuery.SortOrder> sort) {

    public FlightSearchCriteria {
        Objects.requireNonNull(departingFrom, "departingFrom is required");
        sort = List.copyOf(sort);
    }

    public boolean hasOrigin() {
        return origin != null;
    }

    public boolean hasDestination() {
        return destination != null;
    }

    public boolean hasUpperBound() {
        return departingBefore != null;
    }

    public boolean isEmptyWindow() {
        return hasUpperBound() && !departingBefore.isAfter(departingFrom);
    }
}
