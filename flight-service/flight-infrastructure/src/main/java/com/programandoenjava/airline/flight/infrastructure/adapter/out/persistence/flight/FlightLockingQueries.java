package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import java.util.Optional;
import java.util.UUID;

/**
 * The locking read, kept out of the repository interface so it can be written
 * with the criteria API instead of a string.
 */
interface FlightLockingQueries {

    /**
     * Reads a flight for update, so a competing block waits here rather than
     * discovering the conflict at commit (ADR-007).
     */
    Optional<FlightEntity> findByIdForUpdate(UUID id);
}
