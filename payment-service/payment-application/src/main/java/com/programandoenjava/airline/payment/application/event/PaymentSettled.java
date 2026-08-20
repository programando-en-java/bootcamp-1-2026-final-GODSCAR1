package com.programandoenjava.airline.payment.application.event;

import com.programandoenjava.airline.payment.domain.payment.PassengerId;
import com.programandoenjava.airline.payment.domain.payment.Payment;

/**
 * A charge was attempted and answered, either way. In-process only: it carries
 * value objects and is never serialised. What crosses the wire is the flat
 * integration event the listener maps this into (ADR-001).
 *
 * <p>One event rather than two, because Payment already says which happened and
 * splitting it would put that same decision in a second place.
 *
 * <p>The passenger travels beside the payment rather than inside it. A payment
 * is money, and whose booking it settles is booking-service's business; it is
 * here because whoever is told about this has to be told on someone's behalf.
 */
public record PaymentSettled(Payment payment, PassengerId passengerId) {

    public PaymentSettled {
        if (payment == null) {
            throw new IllegalArgumentException("A settled payment is required");
        }
        if (passengerId == null) {
            throw new IllegalArgumentException("A passenger is required");
        }
    }
}
