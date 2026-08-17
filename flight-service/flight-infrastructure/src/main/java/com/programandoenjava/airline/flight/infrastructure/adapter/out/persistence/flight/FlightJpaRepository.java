package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface FlightJpaRepository
        extends JpaRepository<FlightEntity, UUID>, JpaSpecificationExecutor<FlightEntity> {

    /**
     * Reads a flight for update, so a competing block waits here rather than
     * discovering the conflict at commit (ADR-007).
     *
     * <p>The query is written out rather than derived: {@code @Lock} on a
     * derived method does not reliably produce the {@code FOR UPDATE}, and this
     * is not a place to be unsure.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FlightEntity f WHERE f.id = :id")
    Optional<FlightEntity> findByIdForUpdate(@Param("id") UUID id);
}
