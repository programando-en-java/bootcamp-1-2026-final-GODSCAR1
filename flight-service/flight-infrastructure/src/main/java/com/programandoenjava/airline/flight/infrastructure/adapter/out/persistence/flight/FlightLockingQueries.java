package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import java.util.Optional;
import java.util.UUID;

interface FlightLockingQueries {

    Optional<FlightEntity> findByIdForUpdate(UUID id);
}
