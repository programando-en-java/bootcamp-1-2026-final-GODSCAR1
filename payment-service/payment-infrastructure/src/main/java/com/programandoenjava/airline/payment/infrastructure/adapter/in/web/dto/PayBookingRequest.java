package com.programandoenjava.airline.payment.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PayBookingRequest(

        @NotNull(message = "A booking must be named")
        UUID bookingId,

        @NotBlank(message = "A card number is required")
        String cardNumber) {
}
