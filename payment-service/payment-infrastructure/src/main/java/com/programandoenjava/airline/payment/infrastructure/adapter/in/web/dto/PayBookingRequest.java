package com.programandoenjava.airline.payment.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * No amount: what is owed comes from the booking (ADR-013). The card number is
 * validated as a card by the domain, not here, so a bad one is refused with the
 * reason rather than as a pattern mismatch.
 */
public record PayBookingRequest(

        @NotNull(message = "A booking must be named")
        UUID bookingId,

        @NotBlank(message = "A card number is required")
        String cardNumber) {
}
