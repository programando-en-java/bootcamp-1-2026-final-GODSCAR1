package com.programandoenjava.airline.notification.infrastructure.adapter.in.events;

/**
 * The three moments a passenger is told about, one per story in EPIC-05. Named
 * again here rather than shared with the services that publish them (ADR-003):
 * the string is the contract, and a rename on either side has to be noticed.
 */
public final class JourneyTopics {

    public static final String BOOKING_CREATED = "booking.created.v1";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded.v1";
    public static final String CHECK_IN_COMPLETED = "checkin.completed.v1";

    private JourneyTopics() {
    }
}
