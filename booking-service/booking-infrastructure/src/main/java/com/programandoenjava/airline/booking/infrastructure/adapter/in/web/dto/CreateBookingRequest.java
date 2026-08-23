package com.programandoenjava.airline.booking.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/* No passengerId. Whoever the token says is asking is whose booking this is,
 * and a caller can no longer name somebody else. */
public record CreateBookingRequest(

        @NotNull(message = "A flight must be named")
        UUID flightId,

        @NotNull(message = "A number of seats is required")
        @Min(value = 1, message = "At least one seat must be booked")
        @Max(value = 9, message = "A single booking cannot exceed 9 seats")
        Integer seats) {
}
