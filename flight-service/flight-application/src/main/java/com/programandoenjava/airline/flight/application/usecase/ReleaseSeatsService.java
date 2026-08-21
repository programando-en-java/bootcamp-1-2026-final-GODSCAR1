package com.programandoenjava.airline.flight.application.usecase;

import com.programandoenjava.airline.flight.application.port.in.releaseseats.ReleaseSeatsCommand;
import com.programandoenjava.airline.flight.application.port.in.releaseseats.ReleaseSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.out.flight.LockFlightPort;
import com.programandoenjava.airline.flight.application.port.out.flight.SaveFlightPort;
import com.programandoenjava.airline.flight.application.port.out.seatblock.DeleteSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.seatblock.FindSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.shared.exception.FlightNotFoundException;
import com.programandoenjava.airline.flight.application.transaction.UnitOfWork;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;

import java.util.Optional;

public class ReleaseSeatsService implements ReleaseSeatsUseCase {

    private final LockFlightPort lockFlight;
    private final SaveFlightPort saveFlight;
    private final FindSeatBlockPort findSeatBlock;
    private final DeleteSeatBlockPort deleteSeatBlock;

    public ReleaseSeatsService(final LockFlightPort lockFlight,
                               final SaveFlightPort saveFlight,
                               final FindSeatBlockPort findSeatBlock,
                               final DeleteSeatBlockPort deleteSeatBlock) {
        this.lockFlight = lockFlight;
        this.saveFlight = saveFlight;
        this.findSeatBlock = findSeatBlock;
        this.deleteSeatBlock = deleteSeatBlock;
    }

    @Override
    @UnitOfWork
    public void release(final ReleaseSeatsCommand command) {
        Flight flight = lockFlight.byIdForUpdate(command.flightId())
                .orElseThrow(() -> new FlightNotFoundException(command.flightId()));

        Optional<SeatBlock> held =
                findSeatBlock.byIdOnFlight(command.seatBlockId(), command.flightId());
        if (held.isEmpty()) {
            return;
        }

        SeatBlock block = held.get();
        int seats = block.seats().value();
        Flight restored = flight.releaseSeats(seats);

        saveFlight.save(restored);
        deleteSeatBlock.delete(block.id());
    }
}
