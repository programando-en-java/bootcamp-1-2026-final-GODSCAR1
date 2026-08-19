package com.programandoenjava.airline.flight.application.usecase;

import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsCommand;
import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.in.blockseats.SeatsHeld;
import com.programandoenjava.airline.flight.application.port.in.blockseats.exception.BookingAlreadyHoldsSeatsException;
import com.programandoenjava.airline.flight.application.port.shared.exception.FlightNotFoundException;
import com.programandoenjava.airline.flight.application.port.out.seatblock.FindSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.flight.LockFlightPort;
import com.programandoenjava.airline.flight.application.port.out.flight.SaveFlightPort;
import com.programandoenjava.airline.flight.application.port.out.seatblock.SaveSeatBlockPort;
import com.programandoenjava.airline.flight.application.transaction.UnitOfWork;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;
import com.programandoenjava.airline.flight.domain.seatblock.SeatsBlocked;

import java.util.Optional;
import java.time.Clock;

/**
 * Holds seats for a booking, once per request and once per booking.
 *
 * <p>The order of the steps is the whole design. Locking the flight first is
 * what makes the two checks below reliable rather than racy: a competing request
 * for the same flight waits at that line, so by the time it looks for an
 * existing hold the first request has either committed one or rolled back.
 *
 * <p>{@code @UnitOfWork} is what holds the lock from the read to the commit.
 * Without it every port call would open and close its own transaction, the lock
 * would be released the moment the flight was read, and two bookings could
 * oversell the same seats.
 *
 * <p>The price travels back with the block, taken from the flight that was
 * locked. A repeated request answers with the price on that same locked flight,
 * so a retry quotes what the original did unless the fare itself has moved.
 */
public class BlockSeatsService implements BlockSeatsUseCase {

    private final LockFlightPort loadFlight;
    private final SaveFlightPort saveFlight;
    private final FindSeatBlockPort findSeatBlock;
    private final SaveSeatBlockPort saveSeatBlock;
    private final Clock clock;

    public BlockSeatsService(final LockFlightPort loadFlight,
                             final SaveFlightPort saveFlight,
                             final FindSeatBlockPort findSeatBlock,
                             final SaveSeatBlockPort saveSeatBlock,
                             final Clock clock) {
        this.loadFlight = loadFlight;
        this.saveFlight = saveFlight;
        this.findSeatBlock = findSeatBlock;
        this.saveSeatBlock = saveSeatBlock;
        this.clock = clock;
    }

    @Override
    @UnitOfWork
    public SeatsHeld block(final BlockSeatsCommand command) {
        Flight flight = loadFlight.byIdForUpdate(command.flightId())
                .orElseThrow(() -> new FlightNotFoundException(command.flightId()));

        Optional<SeatBlock> sameRequest =
                findSeatBlock.byIdempotencyKey(command.idempotencyKey());
        if (sameRequest.isPresent()) {
            return new SeatsHeld(sameRequest.get(), flight.price());
        }

        if (findSeatBlock.existsForBooking(command.bookingId())) {
            throw new BookingAlreadyHoldsSeatsException(command.bookingId());
        }

        SeatsBlocked blocked =
                flight.block(command.bookingId(), command.seats(), clock.instant());

        saveFlight.save(blocked.flight());
        saveSeatBlock.save(blocked.block(), command.idempotencyKey());

        return new SeatsHeld(blocked.block(), flight.price());
    }
}
