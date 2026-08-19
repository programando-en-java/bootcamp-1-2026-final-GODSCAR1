package com.programandoenjava.airline.payment.domain.payment;

import com.programandoenjava.airline.payment.domain.shared.DomainValidationException;
import com.programandoenjava.airline.payment.domain.shared.Money;

import java.time.Instant;

/**
 * An attempt to charge for a booking, settled either way. A refusal is kept, not
 * discarded: it is what the passenger is told and what the saga announces.
 */
public record Payment(
        PaymentId id,
        BookingId bookingId,
        Money amount,
        PaymentStatus status,
        String cardLastFourDigits,
        Instant processedAt) {

    public Payment {
        if (id == null) {
            throw new DomainValidationException("A payment id is required");
        }
        if (bookingId == null) {
            throw new DomainValidationException("A payment must be for a booking");
        }
        if (amount == null) {
            throw new DomainValidationException("A payment must have an amount");
        }
        if (status == null) {
            throw new DomainValidationException("A payment must have a status");
        }
        if (cardLastFourDigits == null || cardLastFourDigits.length() != 4) {
            throw new DomainValidationException("A payment records the last four digits");
        }
        if (processedAt == null) {
            throw new DomainValidationException("A payment must record when it was made");
        }
    }

    public static Payment succeeded(final PaymentId id,
                                    final BookingId bookingId,
                                    final Money amount,
                                    final CardNumber card,
                                    final Instant now) {
        return settled(id, bookingId, amount, card, PaymentStatus.SUCCEEDED, now);
    }

    public static Payment failed(final PaymentId id,
                                 final BookingId bookingId,
                                 final Money amount,
                                 final CardNumber card,
                                 final Instant now) {
        return settled(id, bookingId, amount, card, PaymentStatus.FAILED, now);
    }

    private static Payment settled(final PaymentId id,
                                   final BookingId bookingId,
                                   final Money amount,
                                   final CardNumber card,
                                   final PaymentStatus status,
                                   final Instant now) {
        String lastFour = card.lastFourDigits();

        return new Payment(id, bookingId, amount, status, lastFour, now);
    }

    public boolean hasSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }
}
