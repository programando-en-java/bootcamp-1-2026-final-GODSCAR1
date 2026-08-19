package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface BoardingSequenceJpaRepository extends JpaRepository<BoardingSequenceEntity, UUID> {

    /**
     * Creates the counter for a flight nobody has checked in for yet. Native
     * because two first passengers arriving together would otherwise both try
     * to insert it and one would fail on the primary key.
     */
    @Modifying
    @Query(value = """
            INSERT INTO boarding_sequences (flight_id, last_sequence)
            VALUES (:flightId, 0)
            ON CONFLICT (flight_id) DO NOTHING
            """, nativeQuery = true)
    int createIfAbsent(@Param("flightId") UUID flightId);

    /**
     * Reads the counter for update, so a second check-in on the same flight
     * waits here instead of being handed a place that is already taken. The
     * query is written out rather than derived, for the reason flight-service
     * gives in its own locking read.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BoardingSequenceEntity s WHERE s.flightId = :flightId")
    Optional<BoardingSequenceEntity> findByFlightForUpdate(@Param("flightId") UUID flightId);
}
