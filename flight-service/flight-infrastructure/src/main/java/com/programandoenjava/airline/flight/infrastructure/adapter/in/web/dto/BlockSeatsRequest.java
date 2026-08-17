package com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BlockSeatsRequest(

        @NotNull(message = "A booking must be named")
        UUID bookingId,

        @NotNull(message = "A number of seats is required")
        @Min(value = 1, message = "At least one seat must be requested")
        @Max(value = 9, message = "A single booking cannot exceed 9 seats")
        Integer seats) {
}
