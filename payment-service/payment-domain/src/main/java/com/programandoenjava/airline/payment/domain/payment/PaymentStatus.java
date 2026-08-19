package com.programandoenjava.airline.payment.domain.payment;

/**
 * No PENDING: the gateway answers within the request, so a payment is settled by
 * the time it exists. A real acquirer that answered later would need one.
 */
public enum PaymentStatus {

    SUCCEEDED,
    FAILED
}
