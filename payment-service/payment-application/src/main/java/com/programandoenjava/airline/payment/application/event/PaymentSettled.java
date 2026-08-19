package com.programandoenjava.airline.payment.application.event;

import com.programandoenjava.airline.payment.domain.payment.Payment;

/**
 * A charge was attempted and answered, either way. In-process only: it carries
 * value objects and is never serialised. What crosses the wire is the flat
 * integration event the listener maps this into (ADR-001).
 *
 * <p>One event rather than two, because Payment already says which happened and
 * splitting it would put that same decision in a second place.
 */
public record PaymentSettled(Payment payment) {

    public PaymentSettled {
        if (payment == null) {
            throw new IllegalArgumentException("A settled payment is required");
        }
    }
}
