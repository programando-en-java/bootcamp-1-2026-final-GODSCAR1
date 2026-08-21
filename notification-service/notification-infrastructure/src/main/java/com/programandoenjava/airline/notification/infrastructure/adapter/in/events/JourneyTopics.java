package com.programandoenjava.airline.notification.infrastructure.adapter.in.events;

public final class JourneyTopics {

    public static final String BOOKING_CREATED = "booking.created.v1";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded.v1";
    public static final String CHECK_IN_COMPLETED = "checkin.completed.v1";

    private JourneyTopics() {
    }
}
