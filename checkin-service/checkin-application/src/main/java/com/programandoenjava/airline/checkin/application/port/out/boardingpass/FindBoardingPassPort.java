package com.programandoenjava.airline.checkin.application.port.out.boardingpass;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;

import java.util.Optional;

public interface FindBoardingPassPort {

    Optional<BoardingPass> byBooking(BookingId bookingId);
}
