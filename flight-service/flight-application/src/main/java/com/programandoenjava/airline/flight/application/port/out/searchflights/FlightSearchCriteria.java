package com.programandoenjava.airline.flight.application.port.out.searchflights;

import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A fully resolved search, ready to be translated into a query.
 *
 * <p>departingFrom always has a value — a search with no lower bound would
 * return flights that have already left. The upper bound is nullable because a
 * search without a date has no end.
 */
public record FlightSearchCriteria(@Nullable AirportCode origin,
                                   @Nullable AirportCode destination,
                                   Instant departingFrom,
                                   @Nullable Instant departingBefore,
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
