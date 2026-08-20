package com.programandoenjava.airline.notification.application.port.out.processedevents;

import java.util.UUID;

/**
 * Delivery is at-least-once, and this is what stops a redelivered message being
 * acted on twice (ADR-014). It matters more here than anywhere else in the
 * system: telling a booking it is confirmed twice changes nothing, and sending
 * a passenger the same notification twice is the thing they notice.
 */
public interface ProcessedEventsPort {

    /** True when this call is the one that claimed the event. */
    boolean claim(UUID eventId);
}
