package com.programandoenjava.airline.flight.application.port.out.searchflights;

import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

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
