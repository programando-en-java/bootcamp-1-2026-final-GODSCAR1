package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.flight.FlightSchedule;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

@DisplayName("Flight schedule")
class FlightScheduleTest {

    private static final Instant DEPARTURE = Instant.parse("2026-09-01T13:00:00Z");
    private static final Instant ARRIVAL = Instant.parse("2026-09-01T14:30:00Z");

    @Test
    @DisplayName("should measure how long the flight takes")
    void shouldMeasureHowLongTheFlightTakes() {
        FlightSchedule schedule = new FlightSchedule(DEPARTURE, ARRIVAL);

        Assertions.assertThat(schedule.duration()).isEqualTo(Duration.ofMinutes(90));
    }

    @Test
    @DisplayName("should refuse a flight that lands before it leaves")
    void shouldRefuseAFlightThatLandsBeforeItLeaves() {
        Assertions.assertThatThrownBy(() -> new FlightSchedule(ARRIVAL, DEPARTURE))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("should still be ahead one second before departure")
    void shouldStillBeAheadOneSecondBeforeDeparture() {
        FlightSchedule schedule = new FlightSchedule(DEPARTURE, ARRIVAL);

        Assertions.assertThat(schedule.departsAfter(DEPARTURE.minusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("should have gone once the departure instant arrives")
    void shouldHaveGoneOnceTheDepartureInstantArrives() {
        FlightSchedule schedule = new FlightSchedule(DEPARTURE, ARRIVAL);

        Assertions.assertThat(schedule.departsAfter(DEPARTURE)).isFalse();
    }
}