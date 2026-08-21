package com.programandoenjava.airline.notification.application.port.out.processedevents;

import java.util.UUID;

public interface ProcessedEventsPort {

    boolean claim(UUID eventId);
}
