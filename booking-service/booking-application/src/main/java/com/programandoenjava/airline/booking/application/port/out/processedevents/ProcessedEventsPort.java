package com.programandoenjava.airline.booking.application.port.out.processedevents;

import java.util.UUID;

public interface ProcessedEventsPort {

    boolean claim(UUID eventId);
}
