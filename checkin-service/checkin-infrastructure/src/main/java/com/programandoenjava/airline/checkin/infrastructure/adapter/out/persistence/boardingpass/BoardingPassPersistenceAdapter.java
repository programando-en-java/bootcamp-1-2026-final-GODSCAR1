package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass;

import com.programandoenjava.airline.checkin.application.port.out.boardingpass.FindBoardingPassPort;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.SaveBoardingPassPort;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
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
        int inserted = insert(boardingPass);

        if (inserted == 1) {
            return Optional.empty();
        }
        return findByBooking(boardingPass.bookingId());
    }

    private Optional<BoardingPass> findByBooking(final BookingId bookingId) {
        return boardingPassJpaRepository.findByBookingId(bookingId.value())
                .map(BoardingPassEntityMapper::toDomain);
    }

    private int insert(final BoardingPass pass) {
        FlightSnapshot flight = pass.flight();

        return boardingPassJpaRepository.insertIfAbsent(
                pass.id().value(),
                pass.bookingId().value(),
                pass.passengerId().value(),
                flight.flightId().value(),
                flight.flightNumber(),
                flight.origin(),
                flight.destination(),
                flight.departure(),
                pass.sequence().value(),
                pass.issuedAt());
    }
}
