package com.programandoenjava.airline.booking.infrastructure.config;

import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingUseCase;
import com.programandoenjava.airline.booking.application.port.out.findbooking.FindBookingPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.application.usecase.CreateBookingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateBookingUseCase createBookingUseCase(final FindBookingPort findBookingPort,
                                              final HoldSeatsPort holdSeatsPort,
                                              final SaveBookingPort saveBookingPort,
                                              final Clock clock) {
        return new CreateBookingService(findBookingPort, holdSeatsPort, saveBookingPort, clock);
    }
}
