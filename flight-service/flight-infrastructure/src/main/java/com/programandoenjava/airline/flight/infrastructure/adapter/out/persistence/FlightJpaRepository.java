package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface FlightJpaRepository
        extends JpaRepository<FlightEntity, UUID>, JpaSpecificationExecutor<FlightEntity> {
}
