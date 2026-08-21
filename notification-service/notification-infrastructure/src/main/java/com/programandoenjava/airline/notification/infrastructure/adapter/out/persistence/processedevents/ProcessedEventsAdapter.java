package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.processedevents;

import com.programandoenjava.airline.notification.application.port.out.processedevents.ProcessedEventsPort;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

class ProcessedEventsAdapter implements ProcessedEventsPort {

    private final ProcessedEventsJpaRepository processedEventsJpaRepository;

    ProcessedEventsAdapter(final ProcessedEventsJpaRepository processedEventsJpaRepository) {
        this.processedEventsJpaRepository = processedEventsJpaRepository;
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    /* Notifying twice is what a passenger notices, so this is claimed before any work. */
    public boolean claim(final UUID eventId) {
        boolean alreadyProcessed = processedEventsJpaRepository.existsById(eventId);

        if (alreadyProcessed) {
            return false;
        }

        processedEventsJpaRepository.save(new ProcessedEventEntity(eventId));

        return true;
    }
}
