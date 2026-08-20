package com.programandoenjava.airline.flight.infrastructure.config;

import com.programandoenjava.airline.flight.application.port.in.blockseats.BlockSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.in.readflight.ReadFlightUseCase;
import com.programandoenjava.airline.flight.application.port.in.releaseseats.ReleaseSeatsUseCase;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsUseCase;
import com.programandoenjava.airline.flight.application.port.out.flight.FindFlightPort;
import com.programandoenjava.airline.flight.application.port.out.flight.LockFlightPort;
import com.programandoenjava.airline.flight.application.port.out.flight.SaveFlightPort;
import com.programandoenjava.airline.flight.application.port.out.searchflights.LoadFlightsPort;
import com.programandoenjava.airline.flight.application.port.out.seatblock.DeleteSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.seatblock.FindSeatBlockPort;
import com.programandoenjava.airline.flight.application.port.out.seatblock.SaveSeatBlockPort;
import com.programandoenjava.airline.flight.application.usecase.BlockSeatsService;
import com.programandoenjava.airline.flight.application.usecase.ReadFlightService;
import com.programandoenjava.airline.flight.application.usecase.ReleaseSeatsService;
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
    SearchFlightsUseCase searchFlightsUseCase(final LoadFlightsPort loadFlightsPort,
                                              final Clock clock) {
        return new SearchFlightsService(loadFlightsPort, clock);
    }

    @Bean
    ReadFlightUseCase readFlightUseCase(final FindFlightPort findFlightPort) {
        return new ReadFlightService(findFlightPort);
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

    @Bean
    ReleaseSeatsUseCase releaseSeatsUseCase(final LockFlightPort lockFlightPort,
                                            final SaveFlightPort saveFlightPort,
                                            final FindSeatBlockPort findSeatBlockPort,
                                            final DeleteSeatBlockPort deleteSeatBlockPort) {
        return new ReleaseSeatsService(lockFlightPort, saveFlightPort,
                findSeatBlockPort, deleteSeatBlockPort);
    }
}
