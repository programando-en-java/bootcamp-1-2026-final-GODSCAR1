package com.programandoenjava.airline.booking.infrastructure.adapter.in.events;

public final class PaymentTopics {

    public static final String SUCCEEDED = "payment.succeeded.v1";
    public static final String FAILED = "payment.failed.v1";

    private PaymentTopics() {
    }
}
