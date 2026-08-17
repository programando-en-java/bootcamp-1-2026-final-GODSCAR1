package com.programandoenjava.airline.booking.application.usecase;

import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingCommand;
import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingUseCase;
import com.programandoenjava.airline.booking.application.port.out.findbooking.FindBookingPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsCommand;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.SeatsHeld;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.domain.booking.BookingId;
import com.programandoenjava.airline.booking.domain.shared.Money;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class CreateBookingService implements CreateBookingUseCase {

    private final FindBookingPort findBooking;
    private final HoldSeatsPort holdSeats;
    private final SaveBookingPort saveBooking;
    private final Clock clock;

    public CreateBookingService(final FindBookingPort findBooking,
                                final HoldSeatsPort holdSeats,
                                final SaveBookingPort saveBooking,
                                final Clock clock) {
        this.findBooking = findBooking;
        this.holdSeats = holdSeats;
        this.saveBooking = saveBooking;
        this.clock = clock;
    }

    @Override
    public Booking create(final CreateBookingCommand command) {
        IdempotencyKey key = command.idempotencyKey();

        Optional<Booking> alreadyMade = findBooking.byIdempotencyKey(key);
        if (alreadyMade.isPresent()) {
            return alreadyMade.get();
        }

        BookingId bookingId = BookingId.newId();

        HoldSeatsCommand hold = new HoldSeatsCommand(
                command.flightId(), bookingId, command.seats(), key);

        SeatsHeld held = holdSeats.hold(hold);

        Instant now = clock.instant();
        Money fare = held.pricePerSeat();

        Booking booking = Booking.of(
                bookingId,
                command.passengerId(),
                command.flightId(),
                held.seatBlockId(),
                command.seats(),
                fare,
                now);

        Optional<Booking> lostTheRace = saveBooking.saveIfNew(booking, key);

        return lostTheRace.orElse(booking);
    }
}
