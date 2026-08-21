package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface BoardingPassJpaRepository extends JpaRepository<BoardingPassEntity, UUID> {

    Optional<BoardingPassEntity> findByBookingId(UUID bookingId);
}
