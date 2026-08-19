package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import com.programandoenjava.airline.checkin.application.port.out.boardingpass.NextBoardingSequencePort;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingSequence;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import org.springframework.transaction.annotation.Transactional;

/**
 * The counter is created on the first check-in for a flight and read for update
 * on every one after. The lock is held until the transaction the caller opened
 * commits, which is what makes the number this returns nobody else's.
 */
class BoardingSequenceAdapter implements NextBoardingSequencePort {

    private final BoardingSequenceJpaRepository boardingSequenceJpaRepository;

    BoardingSequenceAdapter(final BoardingSequenceJpaRepository boardingSequenceJpaRepository) {
        this.boardingSequenceJpaRepository = boardingSequenceJpaRepository;
    }

    @Override
    @Transactional
    public BoardingSequence nextFor(final FlightId flightId) {
        boardingSequenceJpaRepository.createIfAbsent(flightId.value());

        BoardingSequenceEntity counter = boardingSequenceJpaRepository
                .findByFlightForUpdate(flightId.value())
                .orElseThrow(() -> new IllegalStateException(
                        "No boarding sequence for flight " + flightId.value()));

        return new BoardingSequence(counter.takeNext());
    }
}
