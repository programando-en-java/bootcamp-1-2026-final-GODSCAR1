package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.booking.infrastructure.adapter.out.flight.dto.BlockSeatsRequest;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.flight.dto.SeatBlockResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "flightService",
        url = "${airline.flight-service.url}",
        configuration = FlightClientConfiguration.class)
public interface FlightClient {

    @PostMapping("/api/v1/flights/{flightId}/seat-blocks")
    SeatBlockResponse blockSeats(@PathVariable UUID flightId,
                                 @RequestHeader("Idempotency-Key") String idempotencyKey,
                                 @RequestBody BlockSeatsRequest request);
}
