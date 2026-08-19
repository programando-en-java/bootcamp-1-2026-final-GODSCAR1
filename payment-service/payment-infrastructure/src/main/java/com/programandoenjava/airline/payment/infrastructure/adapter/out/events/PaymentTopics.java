package com.programandoenjava.airline.payment.infrastructure.adapter.out.events;

/**
 * One topic per outcome rather than one carrying a status. The consumer does
 * genuinely different work in each case, and a topic per fact lets a future
 * service subscribe to refusals alone.
 */
public final class PaymentTopics {

    public static final String SUCCEEDED = "payment.succeeded.v1";
    public static final String FAILED = "payment.failed.v1";

    private PaymentTopics() {
    }
}
