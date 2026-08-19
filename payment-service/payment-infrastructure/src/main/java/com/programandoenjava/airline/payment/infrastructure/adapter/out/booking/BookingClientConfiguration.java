package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

/** No @Configuration: Feign applies this to one client, not to every bean. */
class BookingClientConfiguration {

    @Bean
    ErrorDecoder bookingErrorDecoder() {
        return new BookingErrorDecoder();
    }
}
