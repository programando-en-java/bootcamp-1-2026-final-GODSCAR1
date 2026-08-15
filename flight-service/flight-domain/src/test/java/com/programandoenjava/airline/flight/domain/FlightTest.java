package com.programandoenjava.airline.flight.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

@DisplayName("Flight")
class FlightTest {

    private static final Instant DEPARTURE = Instant.parse("2026-09-01T13:00:00Z");
    private static final Instant ARRIVAL = Instant.parse("2026-09-01T14:00:00Z");

    @Nested
    @DisplayName("when scheduling a new flight")
    class Scheduling {

        @Test
        @DisplayName("should open every seat for sale")
        void shouldOpenEverySeatForSale() {
            Flight flight = bogotaToMedellin();

            Assertions.assertThat(flight.seats().available()).isEqualTo(120);
        }

        @Test
        @DisplayName("should give it an identity")
        void shouldGiveItAnIdentity() {
            Flight flight = bogotaToMedellin();

            Assertions.assertThat(flight.id()).isNotNull();
        }

        @Test
        @DisplayName("should reject a route that starts and ends at the same airport")
        void shouldRejectARouteThatStartsAndEndsAtTheSameAirport() {
            AirportCode bogota = new AirportCode("BOG");
            FlightSchedule schedule = new FlightSchedule(DEPARTURE, ARRIVAL);

            Assertions.assertThatThrownBy(() -> Flight.schedule(
                            new FlightNumber("AV101"), bogota, bogota,
                            schedule, 120, Money.of("250000.00", "COP")))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("must differ");
        }
    }

    @Nested
    @DisplayName("when checking whether it can be booked")
    class Bookability {

        @Test
        @DisplayName("should be bookable before departure with seats left")
        void shouldBeBookableBeforeDepartureWithSeatsLeft() {
            Flight flight = bogotaToMedellin();
            Instant beforeDeparture = DEPARTURE.minusSeconds(3600);

            Assertions.assertThat(flight.isBookable(beforeDeparture)).isTrue();
        }

        @Test
        @DisplayName("should not be bookable once it has departed")
        void shouldNotBeBookableOnceItHasDeparted() {
            Flight flight = bogotaToMedellin();
            Instant afterDeparture = DEPARTURE.plusSeconds(60);

            Assertions.assertThat(flight.isBookable(afterDeparture)).isFalse();
        }

        @Test
        @DisplayName("should not be bookable when sold out")
        void shouldNotBeBookableWhenSoldOut() {
            Flight soldOut = bogotaToMedellin().blockSeats(120);
            Instant beforeDeparture = DEPARTURE.minusSeconds(3600);

            Assertions.assertThat(soldOut.isBookable(beforeDeparture)).isFalse();
        }
    }

    @Nested
    @DisplayName("when blocking and releasing seats")
    class SeatChanges {

        @Test
        @DisplayName("should return a new flight with fewer seats available")
        void shouldReturnANewFlightWithFewerSeatsAvailable() {
            Flight flight = bogotaToMedellin();

            Flight blocked = flight.blockSeats(2);

            Assertions.assertThat(blocked.seats().available()).isEqualTo(118);
            Assertions.assertThat(flight.seats().available()).isEqualTo(120);
        }

        @Test
        @DisplayName("should keep its identity across changes")
        void shouldKeepItsIdentityAcrossChanges() {
            Flight flight = bogotaToMedellin();

            Flight blocked = flight.blockSeats(2);

            Assertions.assertThat(blocked.id()).isEqualTo(flight.id());
        }

        @Test
        @DisplayName("should come back to the original state after a compensation")
        void shouldComeBackToTheOriginalStateAfterACompensation() {
            Flight flight = bogotaToMedellin();

            Flight compensated = flight.blockSeats(3).releaseSeats(3);

            Assertions.assertThat(compensated).isEqualTo(flight);
        }
    }

    private static Flight bogotaToMedellin() {
        return Flight.schedule(
                new FlightNumber("AV101"),
                new AirportCode("BOG"),
                new AirportCode("MDE"),
                new FlightSchedule(DEPARTURE, ARRIVAL),
                120,
                Money.of("250000.00", "COP"));
    }
}