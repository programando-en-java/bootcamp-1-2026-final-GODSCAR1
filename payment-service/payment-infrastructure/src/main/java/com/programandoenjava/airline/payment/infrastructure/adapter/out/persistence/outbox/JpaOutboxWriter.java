package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox;

import java.time.Instant;
import java.util.UUID;

class JpaOutboxWriter implements OutboxWriter {

    private final OutboxJpaRepository outboxJpaRepository;

    JpaOutboxWriter(final OutboxJpaRepository outboxJpaRepository) {
        this.outboxJpaRepository = outboxJpaRepository;
    }

    /* No transaction of its own: it must join the one already open, not start one. */
    @Override
    public UUID write(final String aggregateType,
                      final String aggregateId,
                      final String topic,
                      final String payload,
                      final Instant now) {
        UUID id = UUID.randomUUID();
        OutboxEntity entity =
                new OutboxEntity(id, aggregateType, aggregateId, topic, payload, now);

        outboxJpaRepository.save(entity);

        return id;
    }
}
