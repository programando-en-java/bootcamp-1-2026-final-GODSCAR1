package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsCommand;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.SeatsHeld;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import com.programandoenjava.airline.booking.domain.shared.Money;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.flight.dto.BlockSeatsRequest;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.flight.dto.SeatBlockResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.Currency;

class HoldSeatsFeignAdapter implements HoldSeatsPort {

    private static final String FLIGHT_SERVICE = "flightService";

    private final FlightClient flightClient;

    HoldSeatsFeignAdapter(final FlightClient flightClient) {
        this.flightClient = flightClient;
    }

    @Override
    @Retry(name = FLIGHT_SERVICE)
    @CircuitBreaker(name = FLIGHT_SERVICE)
    public SeatsHeld hold(final HoldSeatsCommand command) {
        BlockSeatsRequest request = new BlockSeatsRequest(
                command.bookingId().value(), command.seats().value());

        SeatBlockResponse response = flightClient.blockSeats(
                command.flightId().value(),
                command.idempotencyKey().value(),
                request);

        SeatBlockId seatBlockId = new SeatBlockId(response.seatBlockId());
        Currency currency = Currency.getInstance(response.currency());
        Money pricePerSeat = new Money(response.pricePerSeat(), currency);

        return new SeatsHeld(seatBlockId, pricePerSeat);
    }
}
