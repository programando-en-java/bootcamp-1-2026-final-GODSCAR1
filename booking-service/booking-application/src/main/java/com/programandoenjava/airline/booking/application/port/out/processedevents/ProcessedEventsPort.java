package com.programandoenjava.airline.booking.application.port.out.processedevents;

import java.util.UUID;

public interface ProcessedEventsPort {

    /**
     * Claims an event, and says whether this call is the one that got it.
     *
     * <p>False means it has been handled already. Insert-and-see rather than
     * ask-then-insert: two deliveries can arrive at once, and a separate lookup
     * would let both through.
     *
     * @return true if this call claimed the event, false if it was already taken
     */
    boolean claim(UUID eventId);
}
