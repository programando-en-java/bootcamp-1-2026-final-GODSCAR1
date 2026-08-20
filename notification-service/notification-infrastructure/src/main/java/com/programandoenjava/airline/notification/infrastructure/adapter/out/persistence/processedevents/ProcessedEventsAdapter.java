package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.processedevents;

import com.programandoenjava.airline.notification.application.port.out.processedevents.ProcessedEventsPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

class ProcessedEventsAdapter implements ProcessedEventsPort {

    private final ProcessedEventsJpaRepository processedEventsJpaRepository;

    ProcessedEventsAdapter(final ProcessedEventsJpaRepository processedEventsJpaRepository) {
        this.processedEventsJpaRepository = processedEventsJpaRepository;
    }

    @Override
    @Transactional
    public boolean claim(final UUID eventId) {
        return processedEventsJpaRepository.claim(eventId) == 1;
    }
}
