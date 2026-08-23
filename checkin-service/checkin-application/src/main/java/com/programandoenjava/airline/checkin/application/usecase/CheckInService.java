package com.programandoenjava.airline.checkin.application.usecase;

import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInCommand;
import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInUseCase;
import com.programandoenjava.airline.checkin.application.port.in.checkin.exception.BookingNotConfirmedException;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.FindBoardingPassPort;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.BookingToCheckIn;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.checkin.application.port.out.readflight.ReadFlightPort;
import com.programandoenjava.airline.checkin.application.port.shared.Caller;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
import com.programandoenjava.airline.checkin.domain.checkin.CheckInWindow;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

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
        Caller caller = command.caller();

        /* The pass is checked against the caller before it is handed back. This
         * path never reads the booking, so without this a booking id would be
         * enough to read somebody else's boarding pass. */
        Optional<BoardingPass> alreadyIssued = findBoardingPass.byBooking(command.bookingId())
                .filter(pass -> belongsTo(pass, caller));

        if (alreadyIssued.isPresent()) {
            return alreadyIssued.get();
        }

        BookingToCheckIn booking = readBooking.byId(command.bookingId());

        boolean theirs = caller.isStaff() || caller.is(booking.passengerId().value());

        /* Answered as a booking nobody has, not as a refusal: saying no would
         * confirm the booking is real (ADR-022). */
        if (!theirs) {
            throw new BookingNotFoundException(command.bookingId());
        }
        if (!booking.isConfirmed()) {
            throw new BookingNotConfirmedException(command.bookingId(), booking.status());
        }

        FlightSnapshot flight = readFlight.byId(booking.flightId());
        Instant now = clock.instant();

        window.requireOpenAt(flight.departure(), now);

        return issuer.issue(command.bookingId(), booking.passengerId(), flight, now);
    }

    private static boolean belongsTo(final BoardingPass pass, final Caller caller) {
        return caller.isStaff() || caller.is(pass.passengerId().value());
    }
}
