package com.programandoenjava.airline.checkin.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.checkin.application.port.out.readflight.ReadFlightPort;
import com.programandoenjava.airline.checkin.application.port.out.readflight.exception.FlightNotFoundException;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.flight.dto.FlightResponse;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;

class ReadFlightFeignAdapter implements ReadFlightPort {

    private static final String FLIGHT_SERVICE = "flightService";

    private static final int NOT_FOUND = HttpStatus.NOT_FOUND.value();

    private final FlightClient flightClient;

    ReadFlightFeignAdapter(final FlightClient flightClient) {
        this.flightClient = flightClient;
    }

    @Override
    @Retry(name = FLIGHT_SERVICE)
    @CircuitBreaker(name = FLIGHT_SERVICE)
    public FlightSnapshot byId(final FlightId flightId) {
        FlightResponse response = read(flightId);

        return new FlightSnapshot(flightId,
                response.flightNumber(),
                response.origin(),
                response.destination(),
                response.departureTime());
    }

    private FlightResponse read(final FlightId flightId) {
        try {
            return flightClient.byId(flightId.value());
        } catch (FeignException failed) {
            if (failed.status() == NOT_FOUND) {
                throw new FlightNotFoundException(flightId);
            }
            throw failed;
        }
    }
}
