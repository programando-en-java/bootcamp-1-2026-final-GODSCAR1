package com.programandoenjava.airline.checkin.application.port.in.checkin;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;

public interface CheckInUseCase {

    BoardingPass checkIn(CheckInCommand command);
}
