package com.programandoenjava.airline.payment.infrastructure.adapter.out.events;

public final class PaymentTopics {

    public static final String SUCCEEDED = "payment.succeeded.v1";
    public static final String FAILED = "payment.failed.v1";

    private PaymentTopics() {
    }
}
