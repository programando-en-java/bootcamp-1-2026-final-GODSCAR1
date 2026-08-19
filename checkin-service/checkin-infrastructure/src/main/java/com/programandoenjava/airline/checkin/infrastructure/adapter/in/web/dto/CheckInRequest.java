package com.programandoenjava.airline.checkin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * The booking and nothing else. The passenger and the flight are read from the
 * booking, so no caller can name a flight it did not book.
 */
public record CheckInRequest(

        @NotNull(message = "A booking must be named")
        UUID bookingId) {
}
