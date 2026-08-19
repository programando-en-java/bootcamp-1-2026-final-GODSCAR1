package com.programandoenjava.airline.payment.infrastructure.adapter.out.booking;

import com.programandoenjava.airline.payment.application.port.out.readbooking.ReadBookingPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingAdapterConfiguration {

    @Bean
    ReadBookingPort readBookingPort(final BookingClient bookingClient) {
        return new ReadBookingFeignAdapter(bookingClient);
    }
}
