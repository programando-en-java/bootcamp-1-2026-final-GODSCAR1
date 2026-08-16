package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence;

import com.programandoenjava.airline.flight.application.port.in.PageResult;
import com.programandoenjava.airline.flight.application.port.out.FlightSearchCriteria;
import com.programandoenjava.airline.flight.application.port.out.LoadFlightsPort;
import com.programandoenjava.airline.flight.domain.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

class FlightPersistenceAdapter implements LoadFlightsPort {

    private final FlightJpaRepository flightJpaRepository;

    FlightPersistenceAdapter(FlightJpaRepository flightJpaRepository) {
        this.flightJpaRepository = flightJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Flight> search(FlightSearchCriteria criteria) {
        /*
         * A window that cannot contain anything means the requested day is
         * entirely in the past. Returning early avoids a query, and a COUNT,
         * that can only come back empty.
         */
        if (criteria.isEmptyWindow()) {
            return new PageResult<>(List.of(), criteria.page(), criteria.size(), 0);
        }

        Specification<FlightEntity> specification = FlightQueryMapper.toSpecification(criteria);
        PageRequest pageRequest = FlightQueryMapper.toPageRequest(criteria);

        Page<FlightEntity> page = flightJpaRepository.findAll(specification, pageRequest);

        List<Flight> flights = page.getContent().stream()
                .map(FlightEntityMapper::toDomain)
                .toList();

        return new PageResult<>(flights, page.getNumber(), page.getSize(),
                page.getTotalElements());
    }
}