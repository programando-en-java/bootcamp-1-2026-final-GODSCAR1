package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.application.port.out.searchflights.FlightSearchCriteria;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

final class FlightQueryMapper {

    private FlightQueryMapper() {
    }

    static Specification<FlightEntity> toSpecification(final FlightSearchCriteria criteria) {
        Specification<FlightEntity> specification = FlightSpecifications.hasAvailableSeats()
                .and(FlightSpecifications.departingAfter(criteria.departingFrom()));

        if (criteria.hasUpperBound()) {
            specification = specification.and(
                    FlightSpecifications.departingBefore(criteria.departingBefore()));
        }
        if (criteria.hasOrigin()) {
            specification = specification.and(
                    FlightSpecifications.withOrigin(criteria.origin()));
        }
        if (criteria.hasDestination()) {
            specification = specification.and(
                    FlightSpecifications.withDestination(criteria.destination()));
        }
        return specification;
    }

    static PageRequest toPageRequest(final FlightSearchCriteria criteria) {
        List<Sort.Order> orders = criteria.sort().stream()
                .map(FlightQueryMapper::toOrder)
                .toList();

        return PageRequest.of(criteria.page(), criteria.size(), Sort.by(orders));
    }

    private static Sort.Order toOrder(final PageQuery.SortOrder sortOrder) {
        String property = switch (sortOrder.field()) {
            case DEPARTURE_TIME -> FlightEntity_.DEPARTURE_TIME;
            case ARRIVAL_TIME -> FlightEntity_.ARRIVAL_TIME;
            case PRICE -> FlightEntity_.PRICE_AMOUNT;
        };
        Sort.Direction direction = sortOrder.direction() == PageQuery.Direction.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return new Sort.Order(direction, property);
    }
}