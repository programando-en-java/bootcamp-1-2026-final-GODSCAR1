package com.programandoenjava.airline.flight.application.port.in.searchflights;

import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * What a passenger asked for.
 *
 * <p>The three filters are nullable because absent means "do not narrow by
 * this", which is a different thing from an empty value. Page is not: a search
 * without paging would return the whole catalogue.
 */
public record SearchFlightsQuery(@Nullable AirportCode origin,
                                 @Nullable AirportCode destination,
                                 @Nullable LocalDate departureDate,
                                 PageQuery page) {

    public SearchFlightsQuery {
        Objects.requireNonNull(page, "page is required");
    }

    public static SearchFlightsQuery unfiltered(final PageQuery page) {
        return new SearchFlightsQuery(null, null, null, page);
    }
}
