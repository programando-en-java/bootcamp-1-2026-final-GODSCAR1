package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import com.programandoenjava.airline.flight.application.port.out.blockseats.LockFlightPort;
import com.programandoenjava.airline.flight.application.port.out.blockseats.SaveFlightPort;
import com.programandoenjava.airline.flight.application.port.shared.PageResult;
import com.programandoenjava.airline.flight.application.port.out.searchflights.FlightSearchCriteria;
import com.programandoenjava.airline.flight.application.port.out.searchflights.LoadFlightsPort;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.flight.FlightId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

class FlightPersistenceAdapter implements LoadFlightsPort, LockFlightPort, SaveFlightPort {

    private final FlightJpaRepository flightJpaRepository;

    FlightPersistenceAdapter(final FlightJpaRepository flightJpaRepository) {
        this.flightJpaRepository = flightJpaRepository;
    }

    @Override
    public PageResult<Flight> search(final FlightSearchCriteria criteria) {
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

    @Override
    public Optional<Flight> byIdForUpdate(final FlightId id) {
        return flightJpaRepository.findByIdForUpdate(id.value())
                .map(FlightEntityMapper::toDomain);
    }

    @Override
    public void save(final Flight flight) {
        flightJpaRepository.save(FlightEntityMapper.toEntity(flight));
    }
}