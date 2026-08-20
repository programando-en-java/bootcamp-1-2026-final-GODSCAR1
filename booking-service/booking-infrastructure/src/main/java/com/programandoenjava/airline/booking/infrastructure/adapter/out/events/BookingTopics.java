package com.programandoenjava.airline.booking.infrastructure.adapter.out.events;

/**
 * One topic. Confirming and failing a booking are not announced: both happen
 * because a payment message said so, and whoever wants to know already has it
 * from payment-service.
 */
public final class BookingTopics {

    public static final String CREATED = "booking.created.v1";

    private BookingTopics() {
    }
}
