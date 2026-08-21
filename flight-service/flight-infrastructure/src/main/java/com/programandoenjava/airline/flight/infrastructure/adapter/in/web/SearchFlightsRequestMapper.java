package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsQuery;
import com.programandoenjava.airline.flight.application.port.shared.SortableField;
import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto.SearchFlightsRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;

final class SearchFlightsRequestMapper {

    private SearchFlightsRequestMapper() {
    }

    static SearchFlightsQuery toQuery(final SearchFlightsRequest request, final Pageable pageable) {
        PageQuery page = new PageQuery(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                toSortOrders(pageable.getSort()));

        return new SearchFlightsQuery(
                toAirportCode(request.origin()),
                toAirportCode(request.destination()),
                request.date(),
                page);
    }

    private static @Nullable AirportCode toAirportCode(@Nullable final String value) {
        return value == null || value.isBlank() ? null : new AirportCode(value);
    }

    private static List<PageQuery.SortOrder> toSortOrders(final Sort sort) {
        return sort.stream().map(SearchFlightsRequestMapper::toSortOrder).toList();
    }

    private static PageQuery.SortOrder toSortOrder(final Sort.Order order) {
        SortableField field = toField(order.getProperty());
        PageQuery.Direction direction = order.isAscending()
                ? PageQuery.Direction.ASC
                : PageQuery.Direction.DESC;

        return new PageQuery.SortOrder(field, direction);
    }

    private static SortableField toField(final String raw) {
        String normalised = camelToUpperSnake(raw.trim());
        try {
            return SortableField.valueOf(normalised);
        } catch (final IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unknown sort field: " + raw
                            + ". Allowed: departureTime, arrivalTime, price");
        }
    }

    private static String camelToUpperSnake(final String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
    }
}