package com.programandoenjava.airline.booking.infrastructure.adapter.out.events;

import com.programandoenjava.airline.booking.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.outbox.OutboxWriter;
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
    BookingCreatedListener bookingCreatedListener(final OutboxWriter outboxWriter,
                                                  final ObjectMapper objectMapper,
                                                  final Clock clock) {
        return new BookingCreatedListener(outboxWriter, objectMapper, clock);
    }
}
