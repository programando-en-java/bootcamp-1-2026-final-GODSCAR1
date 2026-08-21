package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass;

import com.programandoenjava.airline.checkin.application.port.out.boardingpass.FindBoardingPassPort;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.SaveBoardingPassPort;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

class BoardingPassPersistenceAdapter implements FindBoardingPassPort, SaveBoardingPassPort {

    private final BoardingPassJpaRepository boardingPassJpaRepository;

    BoardingPassPersistenceAdapter(final BoardingPassJpaRepository boardingPassJpaRepository) {
        this.boardingPassJpaRepository = boardingPassJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BoardingPass> byBooking(final BookingId bookingId) {
        return findByBooking(bookingId);
    }

    @Override
    @Transactional
    public Optional<BoardingPass> saveIfNew(final BoardingPass boardingPass) {
        Optional<BoardingPass> alreadyIssued = findByBooking(boardingPass.bookingId());

        if (alreadyIssued.isPresent()) {
            return alreadyIssued;
        }

        BoardingPassEntity entity = BoardingPassEntityMapper.toEntity(boardingPass);

        boardingPassJpaRepository.save(entity);

        return Optional.empty();
    }

    private Optional<BoardingPass> findByBooking(final BookingId bookingId) {
        return boardingPassJpaRepository.findByBookingId(bookingId.value())
                .map(BoardingPassEntityMapper::toDomain);
    }
}
