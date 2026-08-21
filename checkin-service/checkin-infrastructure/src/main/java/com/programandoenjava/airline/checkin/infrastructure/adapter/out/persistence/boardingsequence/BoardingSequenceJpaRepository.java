package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface BoardingSequenceJpaRepository
        extends JpaRepository<BoardingSequenceEntity, UUID>, BoardingSequenceLockingQueries {
}
