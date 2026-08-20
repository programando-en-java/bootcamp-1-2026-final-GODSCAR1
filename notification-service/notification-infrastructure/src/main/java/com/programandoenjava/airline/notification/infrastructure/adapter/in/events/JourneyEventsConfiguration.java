package com.programandoenjava.airline.notification.infrastructure.adapter.in.events;

import com.programandoenjava.airline.notification.application.port.in.notify.NotifyPassengerUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class JourneyEventsConfiguration {

    @Bean
    JourneyEventsListener journeyEventsListener(final NotifyPassengerUseCase notifyPassengerUseCase,
                                                final ObjectMapper objectMapper) {
        return new JourneyEventsListener(notifyPassengerUseCase, objectMapper);
    }
}
