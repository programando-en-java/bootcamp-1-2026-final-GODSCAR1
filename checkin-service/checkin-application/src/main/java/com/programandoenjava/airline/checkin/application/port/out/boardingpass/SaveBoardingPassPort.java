package com.programandoenjava.airline.checkin.application.port.out.boardingpass;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;

import java.util.Optional;

public interface SaveBoardingPassPort {

    /**
     * Empty when this pass was the one written. A pass when another was already
     * there for the same booking, which is the answer the caller wants: a
     * second check-in is not an error, it is the first one again.
     */
    Optional<BoardingPass> saveIfNew(BoardingPass boardingPass);
}
