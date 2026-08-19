package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight;

import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.releaseseats.ReleaseSeatsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlightAdapterConfiguration {

    @Bean
    HoldSeatsPort holdSeatsPort(final FlightClient flightClient) {
        return new HoldSeatsFeignAdapter(flightClient);
    }

    @Bean
    ReleaseSeatsPort releaseSeatsPort(final FlightClient flightClient) {
        return new ReleaseSeatsFeignAdapter(flightClient);
    }
}
