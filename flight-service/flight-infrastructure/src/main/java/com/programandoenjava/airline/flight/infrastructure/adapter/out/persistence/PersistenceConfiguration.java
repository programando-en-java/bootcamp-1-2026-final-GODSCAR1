package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence;

import com.programandoenjava.airline.flight.application.port.out.LoadFlightsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistenceConfiguration {

    @Bean
    LoadFlightsPort loadFlightsPort(FlightJpaRepository flightJpaRepository) {
        return new FlightPersistenceAdapter(flightJpaRepository);
    }
}
