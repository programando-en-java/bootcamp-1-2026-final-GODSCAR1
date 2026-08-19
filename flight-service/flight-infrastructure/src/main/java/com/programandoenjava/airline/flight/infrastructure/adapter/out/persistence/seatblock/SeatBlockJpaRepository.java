package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SeatBlockJpaRepository extends JpaRepository<SeatBlockEntity, UUID> {

    Optional<SeatBlockEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByBookingId(UUID bookingId);

    Optional<SeatBlockEntity> findByIdAndFlightId(UUID id, UUID flightId);
}
