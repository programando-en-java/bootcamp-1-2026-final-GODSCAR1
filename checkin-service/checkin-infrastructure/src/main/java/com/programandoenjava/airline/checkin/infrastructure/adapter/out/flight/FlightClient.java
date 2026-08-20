package com.programandoenjava.airline.checkin.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.checkin.infrastructure.adapter.out.flight.dto.FlightResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "flightService", url = "${airline.flight-service.url}")
public interface FlightClient {

    @GetMapping("/api/v1/flights/{flightId}")
    FlightResponse byId(@PathVariable UUID flightId);
}
