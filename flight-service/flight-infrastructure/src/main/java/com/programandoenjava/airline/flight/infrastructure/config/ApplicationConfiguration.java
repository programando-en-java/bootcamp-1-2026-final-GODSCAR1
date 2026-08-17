package com.programandoenjava.airline.flight.infrastructure.config;

import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsUseCase;
import com.programandoenjava.airline.flight.application.port.out.blockseats.FindSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.blockseats.LockFlightPort;
import com.programandoenjava.airline.flight.application.port.out.blockseats.SaveFlightPort;
import com.programandoenjava.airline.flight.application.port.out.blockseats.SaveSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.searchflights.LoadFlightsPort;
import com.programandoenjava.airline.flight.application.usecase.BlockSeatsService;
import com.programandoenjava.airline.flight.application.usecase.SearchFlightsService;
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
    SearchFlightsUseCase searchFlightsUseCase(final LoadFlightsPort loadFlightsPort, final Clock clock) {
        return new SearchFlightsService(loadFlightsPort, clock);
    }

    @Bean
    BlockSeatsUseCase blockSeatsUseCase(final LockFlightPort lockFlightPort,
                                        final SaveFlightPort saveFlightPort,
                                        final FindSeatBlockPort findSeatBlockPort,
                                        final SaveSeatBlockPort saveSeatBlockPort,
                                        final Clock clock) {
        return new BlockSeatsService(lockFlightPort, saveFlightPort,
                findSeatBlockPort, saveSeatBlockPort, clock);
    }
}
