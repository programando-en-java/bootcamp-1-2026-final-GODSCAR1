package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxConfiguration {

    @Bean
    OutboxWriter outboxWriter(final OutboxJpaRepository outboxJpaRepository) {
        return new JpaOutboxWriter(outboxJpaRepository);
    }

    @Bean
    OutboxRelay outboxRelay(final OutboxJpaRepository outboxJpaRepository,
                            final org.springframework.kafka.core.KafkaTemplate<String, String>
                                    kafkaTemplate,
                            final java.time.Clock clock) {
        return new OutboxRelay(outboxJpaRepository, kafkaTemplate, clock);
    }
}
