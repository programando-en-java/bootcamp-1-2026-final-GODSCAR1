package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.processedevents;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProcessedEventsConfiguration {

    @Bean
    ProcessedEventsAdapter processedEventsAdapter(
            final ProcessedEventsJpaRepository processedEventsJpaRepository) {
        return new ProcessedEventsAdapter(processedEventsJpaRepository);
    }
}
