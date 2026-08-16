package com.programandoenjava.airline.flight.infrastructure.config;

import com.programandoenjava.airline.flight.application.port.in.SearchFlightsUseCase;
import com.programandoenjava.airline.flight.application.port.out.LoadFlightsPort;
import com.programandoenjava.airline.flight.application.usecase.SearchFlightsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class FlightConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SearchFlightsUseCase searchFlightsUseCase(LoadFlightsPort loadFlightsPort, Clock clock) {
        return new SearchFlightsService(loadFlightsPort, clock);
    }
}
