package com.programandoenjava.airline.checkin.application.port.out.boardingsequence;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingSequence;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;

/**
 * Hands out the next place in a flight's boarding order. Two passengers asking
 * at the same moment must not get the same number, so this is one statement and
 * not a read followed by a write.
 */
public interface NextBoardingSequencePort {

    BoardingSequence forFlight(FlightId flightId);
}
