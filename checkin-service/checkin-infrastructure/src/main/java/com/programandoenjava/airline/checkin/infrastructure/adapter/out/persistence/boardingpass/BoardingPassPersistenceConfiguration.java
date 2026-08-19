package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BoardingPassPersistenceConfiguration {

    @Bean
    BoardingPassPersistenceAdapter boardingPassPersistenceAdapter(
            final BoardingPassJpaRepository boardingPassJpaRepository) {

        return new BoardingPassPersistenceAdapter(boardingPassJpaRepository);
    }
}
