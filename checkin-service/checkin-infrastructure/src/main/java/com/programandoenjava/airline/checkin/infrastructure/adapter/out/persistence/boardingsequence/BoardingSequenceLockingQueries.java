package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import java.util.Optional;
import java.util.UUID;

interface BoardingSequenceLockingQueries {

    Optional<BoardingSequenceEntity> findByFlightForUpdate(UUID flightId);
}
