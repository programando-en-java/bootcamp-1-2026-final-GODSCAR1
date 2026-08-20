package com.programandoenjava.airline.checkin.application.usecase;

import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInCommand;
import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInUseCase;
import com.programandoenjava.airline.checkin.application.port.in.checkin.exception.BookingNotConfirmedException;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.FindBoardingPassPort;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.BookingToCheckIn;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.checkin.application.port.out.readflight.ReadFlightPort;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
import com.programandoenjava.airline.checkin.domain.checkin.CheckInWindow;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/*
 * Both halves of the epic are here. US-007 is the pass at the end; US-008 is
 * everything that can stop it: an unconfirmed booking, and a window that has
 * not opened, has closed, or belongs to a flight that has gone.
 *
 * The pass already issued is looked for first, before either network call. A
 * passenger who reloads the page is the common case, and it costs two requests
 * to two services to discover what one row already knows.
 *
 * No transaction here: reading the booking and the flight are network calls,
 * and the writes that must be atomic are behind BoardingPassIssuer.
 */
public class CheckInService implements CheckInUseCase {

    private final FindBoardingPassPort findBoardingPass;
    private final ReadBookingPort readBooking;
    private final ReadFlightPort readFlight;
    private final BoardingPassIssuer issuer;
    private final CheckInWindow window;
    private final Clock clock;

    public CheckInService(final FindBoardingPassPort findBoardingPass,
                          final ReadBookingPort readBooking,
                          final ReadFlightPort readFlight,
                          final BoardingPassIssuer issuer,
                          final CheckInWindow window,
                          final Clock clock) {
        this.findBoardingPass = findBoardingPass;
        this.readBooking = readBooking;
        this.readFlight = readFlight;
        this.issuer = issuer;
        this.window = window;
        this.clock = clock;
    }

    @Override
    public BoardingPass checkIn(final CheckInCommand command) {
        Optional<BoardingPass> alreadyIssued = findBoardingPass.byBooking(command.bookingId());
        if (alreadyIssued.isPresent()) {
            return alreadyIssued.get();
        }

        BookingToCheckIn booking = readBooking.byId(command.bookingId());
        if (!booking.isConfirmed()) {
            throw new BookingNotConfirmedException(command.bookingId(), booking.status());
        }

        FlightSnapshot flight = readFlight.byId(booking.flightId());
        Instant now = clock.instant();

        window.requireOpenAt(flight.departure(), now);

        return issuer.issue(command.bookingId(), booking.passengerId(), flight, now);
    }
}
