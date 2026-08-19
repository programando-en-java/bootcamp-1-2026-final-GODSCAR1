package com.programandoenjava.airline.payment.application.port.in.paybooking;

import com.programandoenjava.airline.payment.domain.payment.BookingId;
import com.programandoenjava.airline.payment.domain.payment.CardNumber;

/**
 * No amount: what is owed comes from the booking, not from whoever is paying it
 * (ADR-013).
 */
public record PayBookingCommand(BookingId bookingId, CardNumber card) {

    public PayBookingCommand {
        if (bookingId == null) {
            throw new IllegalArgumentException("A booking must be named");
        }
        if (card == null) {
            throw new IllegalArgumentException("A card is required");
        }
    }
}
