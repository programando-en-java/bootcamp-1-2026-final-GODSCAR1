package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.processedevents;

import com.programandoenjava.airline.booking.application.port.out.processedevents.ProcessedEventsPort;
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
    /* No retry, and none needed: a refusal fails the listener and the broker delivers
     * again, so the redelivery is the retry. */
    public boolean claim(final UUID eventId) {
        boolean alreadyProcessed = processedEventsJpaRepository.existsById(eventId);

        if (alreadyProcessed) {
            return false;
        }

        processedEventsJpaRepository.save(new ProcessedEventEntity(eventId));

        return true;
    }
}
