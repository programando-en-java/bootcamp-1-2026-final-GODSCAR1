package com.programandoenjava.airline.checkin.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.checkin.application.port.out.readflight.ReadFlightPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlightAdapterConfiguration {

    @Bean
    ReadFlightPort readFlightPort(final FlightClient flightClient) {
        return new ReadFlightFeignAdapter(flightClient);
    }
}
