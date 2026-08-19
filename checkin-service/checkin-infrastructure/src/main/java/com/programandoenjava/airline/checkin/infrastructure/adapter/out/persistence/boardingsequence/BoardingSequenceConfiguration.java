package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BoardingSequenceConfiguration {

    @Bean
    BoardingSequenceAdapter boardingSequenceAdapter(
            final BoardingSequenceJpaRepository boardingSequenceJpaRepository) {

        return new BoardingSequenceAdapter(boardingSequenceJpaRepository);
    }
}
