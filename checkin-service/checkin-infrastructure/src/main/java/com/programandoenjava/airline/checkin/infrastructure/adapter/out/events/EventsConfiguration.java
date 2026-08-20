package com.programandoenjava.airline.checkin.infrastructure.adapter.out.events;

import com.programandoenjava.airline.checkin.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox.OutboxWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@Configuration
public class EventsConfiguration {

    @Bean
    DomainEventPublisher domainEventPublisher(final ApplicationEventPublisher publisher) {
        return new SpringDomainEventPublisher(publisher);
    }

    @Bean
    CheckInCompletedListener checkInCompletedListener(final OutboxWriter outboxWriter,
                                                      final ObjectMapper objectMapper,
                                                      final Clock clock) {
        return new CheckInCompletedListener(outboxWriter, objectMapper, clock);
    }
}
