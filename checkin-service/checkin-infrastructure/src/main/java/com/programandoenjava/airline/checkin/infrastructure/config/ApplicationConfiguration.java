package com.programandoenjava.airline.checkin.infrastructure.config;

import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInUseCase;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.FindBoardingPassPort;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.NextBoardingSequencePort;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.SaveBoardingPassPort;
import com.programandoenjava.airline.checkin.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.checkin.application.port.out.readflight.ReadFlightPort;
import com.programandoenjava.airline.checkin.application.usecase.BoardingPassIssuer;
import com.programandoenjava.airline.checkin.application.usecase.CheckInService;
import com.programandoenjava.airline.checkin.domain.checkin.CheckInWindow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * The rule lives in the domain and the two numbers live here, so an airline
     * that opens check-in earlier changes configuration rather than code.
     */
    @Bean
    CheckInWindow checkInWindow(
            @Value("${airline.checkin.opens-before}") final Duration opensBefore,
            @Value("${airline.checkin.closes-before}") final Duration closesBefore) {

        return new CheckInWindow(opensBefore, closesBefore);
    }

    @Bean
    BoardingPassIssuer boardingPassIssuer(final NextBoardingSequencePort nextBoardingSequencePort,
                                          final SaveBoardingPassPort saveBoardingPassPort,
                                          final DomainEventPublisher domainEventPublisher) {
        return new BoardingPassIssuer(nextBoardingSequencePort, saveBoardingPassPort,
                domainEventPublisher);
    }

    @Bean
    CheckInUseCase checkInUseCase(final FindBoardingPassPort findBoardingPassPort,
                                  final ReadBookingPort readBookingPort,
                                  final ReadFlightPort readFlightPort,
                                  final BoardingPassIssuer boardingPassIssuer,
                                  final CheckInWindow checkInWindow,
                                  final Clock clock) {
        return new CheckInService(findBoardingPassPort, readBookingPort, readFlightPort,
                boardingPassIssuer, checkInWindow, clock);
    }
}
