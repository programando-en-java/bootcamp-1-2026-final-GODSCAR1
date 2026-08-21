package com.programandoenjava.airline.checkin.application.port.out.boardingpass;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingSequence;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;

public interface NextBoardingSequencePort {

    BoardingSequence nextFor(FlightId flightId);
}
