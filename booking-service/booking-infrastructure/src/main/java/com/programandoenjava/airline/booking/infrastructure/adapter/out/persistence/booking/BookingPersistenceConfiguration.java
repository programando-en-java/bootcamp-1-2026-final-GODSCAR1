package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingPersistenceConfiguration {

    @Bean
    BookingPersistenceAdapter bookingPersistenceAdapter(
            final BookingJpaRepository bookingJpaRepository) {
        return new BookingPersistenceAdapter(bookingJpaRepository);
    }
}
