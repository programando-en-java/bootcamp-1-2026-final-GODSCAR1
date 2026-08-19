package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.booking.application.port.out.releaseseats.ReleaseSeatsPort;
import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/*
 * No error translation: flight-service answers 204 for a hold that is not there,
 * so the only failures left are technical ones, and those are what the retry is
 * for. If they outlast it the exception propagates, the booking keeps its unset
 * seats_released_at, and a sweep would find it.
 */
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
