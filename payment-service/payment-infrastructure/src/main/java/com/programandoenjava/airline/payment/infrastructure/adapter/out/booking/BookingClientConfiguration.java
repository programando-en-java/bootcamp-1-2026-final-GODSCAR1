package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

class BookingClientConfiguration {

    @Bean
    ErrorDecoder bookingErrorDecoder() {
        return new BookingErrorDecoder();
    }
}
