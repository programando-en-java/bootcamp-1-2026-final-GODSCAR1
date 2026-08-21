package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import com.programandoenjava.airline.flight.application.port.out.flight.FindFlightPort;
import com.programandoenjava.airline.flight.application.port.out.flight.LockFlightPort;
import com.programandoenjava.airline.flight.application.port.out.flight.SaveFlightPort;
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

class FlightPersistenceAdapter
        implements LoadFlightsPort, FindFlightPort, LockFlightPort, SaveFlightPort {

    private final FlightJpaRepository flightJpaRepository;

    FlightPersistenceAdapter(final FlightJpaRepository flightJpaRepository) {
        this.flightJpaRepository = flightJpaRepository;
    }

    @Override
    public PageResult<Flight> search(final FlightSearchCriteria criteria) {
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
    public Optional<Flight> byId(final FlightId id) {
        return flightJpaRepository.findById(id.value())
                .map(FlightEntityMapper::toDomain);
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