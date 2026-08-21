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

    /**
     * Looks before it writes, which read committed would let two deliveries of
     * the same message do at once. Serialisable is what stops them: the second
     * is refused by the database rather than allowed to insert a row the first
     * one is already inserting.
     *
     * <p>Nothing retries here, and nothing needs to. A refusal fails the
     * listener, the broker delivers the message again, and the second time the
     * row is there and this answers false (ADR-014).
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean claim(final UUID eventId) {
        boolean alreadyProcessed = processedEventsJpaRepository.existsById(eventId);

        if (alreadyProcessed) {
            return false;
        }

        processedEventsJpaRepository.save(new ProcessedEventEntity(eventId));

        return true;
    }
}
