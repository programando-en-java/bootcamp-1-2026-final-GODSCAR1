package com.programandoenjava.airline.checkin.application.port.out.boardingpass;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingSequence;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;

/**
 * Hands out the next place in one flight's boarding order. Two passengers
 * checking in at the same moment must not get the same number, which is why
 * this is a port and not a count.
 */
public interface NextBoardingSequencePort {

    BoardingSequence nextFor(FlightId flightId);
}
