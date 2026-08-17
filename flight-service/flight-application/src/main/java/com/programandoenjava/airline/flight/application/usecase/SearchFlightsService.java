package com.programandoenjava.airline.flight.application.usecase;

import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsQuery;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsUseCase;
import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.application.port.shared.PageResult;
import com.programandoenjava.airline.flight.application.port.shared.SortableField;
import com.programandoenjava.airline.flight.application.port.out.searchflights.FlightSearchCriteria;
import com.programandoenjava.airline.flight.application.port.out.searchflights.LoadFlightsPort;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public class SearchFlightsService implements SearchFlightsUseCase {

    private static final List<PageQuery.SortOrder> DEFAULT_SORT = List.of(
            new PageQuery.SortOrder(SortableField.DEPARTURE_TIME, PageQuery.Direction.ASC));

    private final LoadFlightsPort loadFlightsPort;
    private final Clock clock;

    public SearchFlightsService(final LoadFlightsPort loadFlightsPort, final Clock clock) {
        this.loadFlightsPort = loadFlightsPort;
        this.clock = clock;
    }

    @Override
    public PageResult<Flight> search(final SearchFlightsQuery query) {
        Instant now = clock.instant();
        LocalDate departureDate = query.departureDate();

        Instant departingFrom = lowerBound(departureDate, now);
        Instant departingBefore = upperBound(departureDate);

        PageQuery page = query.page();
        List<PageQuery.SortOrder> sort = page.isUnsorted() ? DEFAULT_SORT : page.sort();

        FlightSearchCriteria criteria = new FlightSearchCriteria(
                query.origin(),
                query.destination(),
                departingFrom,
                departingBefore,
                page.page(),
                page.size(),
                sort);

        return loadFlightsPort.search(criteria);
    }

    /*
     * Without a date the search starts now. With one it starts at midnight, but
     * never earlier than now: asking for today must not return this morning's
     * departures. That clamp is where the two acceptance criteria meet.
     */
    private Instant lowerBound(@Nullable final LocalDate date, final Instant now) {
        if (date == null) {
            return now;
        }
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        return startOfDay.isAfter(now) ? startOfDay : now;
    }

    private @Nullable Instant upperBound(@Nullable final LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
