package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox;

import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Sends what the outbox holds, then marks it sent.
 *
 * <p>Deliberately generic: it knows nothing about check-in, only that a row has
 * a topic, a key and a payload. Keying by aggregate id puts every message about
 * one booking on the same partition, which is what keeps them in order.
 *
 * <p>Delivery is at-least-once. If Kafka accepts a message and the update that
 * follows does not land, the message goes out again on the next sweep, which is
 * why every consumer has to be idempotent (ADR-001).
 */
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;

    private final OutboxJpaRepository outboxJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    public OutboxRelay(final OutboxJpaRepository outboxJpaRepository,
                       final KafkaTemplate<String, String> kafkaTemplate,
                       final Clock clock) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${airline.outbox.poll-interval:PT1S}")
    @Transactional
    public void publishPending() {
        List<OutboxEntity> pending = outboxJpaRepository.claimPending(Limit.of(BATCH_SIZE));
        if (pending.isEmpty()) {
            return;
        }

        Instant now = clock.instant();
        for (OutboxEntity message : pending) {
            send(message);
            message.markPublished(now);
        }
    }

    /*
     * Waits for the broker rather than firing and forgetting. send returns a
     * future, and marking a row sent before that future completes would claim
     * delivery the broker never confirmed, which is also what acks=all is
     * configured for.
     */
    private void send(final OutboxEntity message) {
        String topic = message.getTopic();
        String key = message.getAggregateId();
        String payload = message.getPayload();

        kafkaTemplate.send(topic, key, payload).join();
    }
}
