package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the seat block adapter, in the package that holds it so that the adapter
 * and its repository stay package-private.
 */
@Configuration
public class SeatBlockPersistenceConfiguration {

    @Bean
    SeatBlockPersistenceAdapter seatBlockPersistenceAdapter(
            final SeatBlockJpaRepository seatBlockJpaRepository) {
        return new SeatBlockPersistenceAdapter(seatBlockJpaRepository);
    }
}
