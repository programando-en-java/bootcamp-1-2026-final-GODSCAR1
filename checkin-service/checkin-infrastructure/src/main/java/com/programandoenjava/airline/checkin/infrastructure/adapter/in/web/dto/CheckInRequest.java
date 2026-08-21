package com.programandoenjava.airline.checkin.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckInRequest(

        @NotNull(message = "A booking must be named")
        UUID bookingId) {
}
