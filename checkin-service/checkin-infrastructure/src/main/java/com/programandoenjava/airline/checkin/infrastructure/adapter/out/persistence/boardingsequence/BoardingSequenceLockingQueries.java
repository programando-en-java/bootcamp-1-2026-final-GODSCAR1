package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import java.util.Optional;
import java.util.UUID;

/**
 * The locking read, kept out of the repository interface so it can be written
 * with the criteria API instead of a string.
 */
interface BoardingSequenceLockingQueries {

    /**
     * Reads the counter for update, so a second check-in on the same flight
     * waits here instead of being handed a place that is already taken.
     */
    Optional<BoardingSequenceEntity> findByFlightForUpdate(UUID flightId);
}
