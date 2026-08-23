package com.programandoenjava.airline.checkin.infrastructure.adapter.out.booking;

import com.programandoenjava.airline.checkin.application.port.out.readbooking.ReadBookingPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingAdapterConfiguration {

    @Bean
    BearerTokenRelay bearerTokenRelay() {
        return new BearerTokenRelay();
    }

    @Bean
    ReadBookingPort readBookingPort(final BookingClient bookingClient) {
        return new ReadBookingFeignAdapter(bookingClient);
    }
}
