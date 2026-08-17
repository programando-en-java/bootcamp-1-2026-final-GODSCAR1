package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlightPersistenceConfiguration {

    @Bean
    FlightPersistenceAdapter flightPersistenceAdapter(final FlightJpaRepository flightJpaRepository) {
        return new FlightPersistenceAdapter(flightJpaRepository);
    }
}
