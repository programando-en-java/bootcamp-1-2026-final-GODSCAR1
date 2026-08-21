package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

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
        List<OutboxEntity> pending = outboxJpaRepository.claimPending(BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        Instant now = clock.instant();
        for (OutboxEntity message : pending) {
            send(message);
            message.markPublished(now);
        }
    }

    private void send(final OutboxEntity message) {
        String topic = message.getTopic();
        String key = message.getAggregateId();
        String payload = message.getPayload();

        /* Waits for the broker, so a row is never marked sent on a delivery it never confirmed. */
        kafkaTemplate.send(topic, key, payload).join();
    }
}
