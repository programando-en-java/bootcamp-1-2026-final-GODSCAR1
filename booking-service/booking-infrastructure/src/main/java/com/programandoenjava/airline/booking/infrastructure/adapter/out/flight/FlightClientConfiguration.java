package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

class FlightClientConfiguration {

    @Bean
    ErrorDecoder flightErrorDecoder() {
        return new FlightErrorDecoder();
    }
}
