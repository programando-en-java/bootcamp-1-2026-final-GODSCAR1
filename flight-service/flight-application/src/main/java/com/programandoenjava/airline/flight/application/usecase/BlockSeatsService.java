package com.programandoenjava.airline.flight.application.usecase;

import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsCommand;
import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.in.blockseats.exception.BookingAlreadyHoldsSeatsException;
import com.programandoenjava.airline.flight.application.port.in.blockseats.exception.FlightNotFoundException;
import com.programandoenjava.airline.flight.application.port.out.blockseats.*;
import com.programandoenjava.airline.flight.application.transaction.UnitOfWork;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;
import com.programandoenjava.airline.flight.domain.seatblock.SeatsBlocked;

import java.time.Clock;
import java.util.Optional;

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
    public SeatBlock block(final BlockSeatsCommand command) {
        Flight flight = loadFlight.byIdForUpdate(command.flightId())
                .orElseThrow(() -> new FlightNotFoundException(command.flightId()));

        Optional<SeatBlock> sameRequest = findSeatBlock.byIdempotencyKey(command.idempotencyKey());
        if (sameRequest.isPresent()) {
            return sameRequest.get();
        }

        if (findSeatBlock.existsForBooking(command.bookingId())) {
            throw new BookingAlreadyHoldsSeatsException(command.bookingId());
        }

        SeatsBlocked blocked = flight.block(command.bookingId(), command.seats(), clock.instant());

        saveFlight.save(blocked.flight());
        saveSeatBlock.save(blocked.block(), command.idempotencyKey());

        return blocked.block();
    }
}
