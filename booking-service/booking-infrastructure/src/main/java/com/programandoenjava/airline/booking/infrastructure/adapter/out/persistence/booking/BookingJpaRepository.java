package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);
}
