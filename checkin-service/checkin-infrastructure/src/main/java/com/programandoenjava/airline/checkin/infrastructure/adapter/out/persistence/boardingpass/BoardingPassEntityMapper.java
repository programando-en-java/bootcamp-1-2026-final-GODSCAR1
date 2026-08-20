package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPassId;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingSequence;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
import com.programandoenjava.airline.checkin.domain.boardingpass.PassengerId;

final class BoardingPassEntityMapper {

    private BoardingPassEntityMapper() {
    }

    static BoardingPass toDomain(final BoardingPassEntity entity) {
        FlightSnapshot flight = new FlightSnapshot(
                new FlightId(entity.getFlightId()),
                entity.getFlightNumber(),
                entity.getOrigin(),
                entity.getDestination(),
                entity.getDepartureTime());

        return new BoardingPass(
                new BoardingPassId(entity.getId()),
                new BookingId(entity.getBookingId()),
                new PassengerId(entity.getPassengerId()),
                flight,
                new BoardingSequence(entity.getBoardingSequence()),
                entity.getIssuedAt());
    }
}
