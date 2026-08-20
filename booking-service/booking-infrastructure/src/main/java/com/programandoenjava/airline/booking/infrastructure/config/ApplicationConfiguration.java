package com.programandoenjava.airline.booking.infrastructure.config;

import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.readbooking.ReadBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.ConfirmBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.FailBookingUseCase;
import com.programandoenjava.airline.booking.application.port.out.findbooking.FindBookingPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.booking.application.port.out.processedevents.ProcessedEventsPort;
import com.programandoenjava.airline.booking.application.port.out.releaseseats.ReleaseSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.application.usecase.BookingRecorder;
import com.programandoenjava.airline.booking.application.usecase.ConfirmBookingService;
import com.programandoenjava.airline.booking.application.usecase.CreateBookingService;
import com.programandoenjava.airline.booking.application.usecase.FailBookingService;
import com.programandoenjava.airline.booking.application.usecase.ReadBookingService;
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
    BookingRecorder bookingRecorder(final SaveBookingPort saveBookingPort,
                                    final DomainEventPublisher domainEventPublisher) {
        return new BookingRecorder(saveBookingPort, domainEventPublisher);
    }

    @Bean
    CreateBookingUseCase createBookingUseCase(final FindBookingPort findBookingPort,
                                              final HoldSeatsPort holdSeatsPort,
                                              final BookingRecorder bookingRecorder,
                                              final Clock clock) {
        return new CreateBookingService(findBookingPort, holdSeatsPort, bookingRecorder, clock);
    }

    @Bean
    ReadBookingUseCase readBookingUseCase(final FindBookingPort findBookingPort) {
        return new ReadBookingService(findBookingPort);
    }

    @Bean
    ConfirmBookingUseCase confirmBookingUseCase(final ProcessedEventsPort processedEventsPort,
                                                final FindBookingPort findBookingPort,
                                                final SaveBookingPort saveBookingPort) {
        return new ConfirmBookingService(processedEventsPort, findBookingPort, saveBookingPort);
    }

    @Bean
    FailBookingUseCase failBookingUseCase(final ProcessedEventsPort processedEventsPort,
                                          final FindBookingPort findBookingPort,
                                          final SaveBookingPort saveBookingPort,
                                          final ReleaseSeatsPort releaseSeatsPort,
                                          final Clock clock) {
        return new FailBookingService(processedEventsPort, findBookingPort,
                saveBookingPort, releaseSeatsPort, clock);
    }
}
