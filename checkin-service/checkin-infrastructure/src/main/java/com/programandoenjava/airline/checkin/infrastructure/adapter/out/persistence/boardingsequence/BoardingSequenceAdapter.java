package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import com.programandoenjava.airline.checkin.application.port.out.boardingpass.NextBoardingSequencePort;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingSequence;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import org.springframework.transaction.annotation.Transactional;

class BoardingSequenceAdapter implements NextBoardingSequencePort {

    private final BoardingSequenceJpaRepository boardingSequenceJpaRepository;

    BoardingSequenceAdapter(final BoardingSequenceJpaRepository boardingSequenceJpaRepository) {
        this.boardingSequenceJpaRepository = boardingSequenceJpaRepository;
    }

    @Override
    @Transactional
    public BoardingSequence nextFor(final FlightId flightId) {
        BoardingSequenceEntity counter = boardingSequenceJpaRepository
                .findByFlightForUpdate(flightId.value())
                .orElseGet(() -> create(flightId));

        return new BoardingSequence(counter.takeNext());
    }

    /* Two first passengers can both find no counter. The serialisable unit of work
     * this runs inside is what stops them both writing one. */
    private BoardingSequenceEntity create(final FlightId flightId) {
        BoardingSequenceEntity counter = new BoardingSequenceEntity(flightId.value());

        return boardingSequenceJpaRepository.save(counter);
    }
}
