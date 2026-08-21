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
    /* Read committed on purpose (ADR-019). The lock on the flight row orders the losers
     * by making them wait until they can see what the winner wrote, which is how they
     * answer 409. Serialisable aborts them instead. */
    public SeatsHeld block(final BlockSeatsCommand command) {
        Flight flight = loadFlight.byIdForUpdate(command.flightId())
                .orElseThrow(() -> new FlightNotFoundException(command.flightId()));

        Optional<SeatBlock> sameRequest =
                findSeatBlock.byIdempotencyKey(command.idempotencyKey());
        if (sameRequest.isPresent()) {
            return new SeatsHeld(sameRequest.get(), flight.price());
        }

        boolean alreadyHolds = findSeatBlock.existsForBooking(command.bookingId());

        if (alreadyHolds) {
            throw new BookingAlreadyHoldsSeatsException(command.bookingId());
        }

        SeatsBlocked blocked =
                flight.block(command.bookingId(), command.seats(), clock.instant());

        saveFlight.save(blocked.flight());
        saveSeatBlock.save(blocked.block(), command.idempotencyKey());

        return new SeatsHeld(blocked.block(), flight.price());
    }
}
