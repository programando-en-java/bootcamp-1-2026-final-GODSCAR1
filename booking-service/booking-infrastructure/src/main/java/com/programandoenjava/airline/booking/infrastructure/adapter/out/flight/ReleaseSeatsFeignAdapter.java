package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.booking.application.port.out.releaseseats.ReleaseSeatsPort;
import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

class ReleaseSeatsFeignAdapter implements ReleaseSeatsPort {

    private static final String FLIGHT_SERVICE = "flightService";

    private final FlightClient flightClient;

    ReleaseSeatsFeignAdapter(final FlightClient flightClient) {
        this.flightClient = flightClient;
    }

    @Override
    @Retry(name = FLIGHT_SERVICE)
    @CircuitBreaker(name = FLIGHT_SERVICE)
    public void release(final FlightId flightId, final SeatBlockId seatBlockId) {
        flightClient.releaseSeats(flightId.value(), seatBlockId.value());
    }
}
