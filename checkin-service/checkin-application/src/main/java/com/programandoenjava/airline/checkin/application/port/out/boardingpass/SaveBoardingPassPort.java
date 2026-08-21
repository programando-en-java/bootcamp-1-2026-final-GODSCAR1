package com.programandoenjava.airline.checkin.application.port.out.boardingpass;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;

import java.util.Optional;

public interface SaveBoardingPassPort {

    /* Empty means this call wrote it. A pass means one was already issued. */
    Optional<BoardingPass> saveIfNew(BoardingPass boardingPass);
}
