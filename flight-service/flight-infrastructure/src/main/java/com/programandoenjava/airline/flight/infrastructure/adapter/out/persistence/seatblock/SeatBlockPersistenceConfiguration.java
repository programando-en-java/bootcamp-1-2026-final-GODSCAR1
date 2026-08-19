package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeatBlockPersistenceConfiguration {

    @Bean
    SeatBlockPersistenceAdapter seatBlockPersistenceAdapter(
            final SeatBlockJpaRepository seatBlockJpaRepository) {
        return new SeatBlockPersistenceAdapter(seatBlockJpaRepository);
    }
}
