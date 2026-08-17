package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.flight.Flight;
import com.programandoenjava.airline.flight.domain.flight.FlightNumber;
import com.programandoenjava.airline.flight.domain.flight.FlightSchedule;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatCount;
import com.programandoenjava.airline.flight.domain.seatblock.SeatsBlocked;
import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import com.programandoenjava.airline.flight.domain.shared.Money;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

@DisplayName("Flight")
class FlightTest {

    private static final Instant DEPARTURE = Instant.parse("2026-09-01T13:00:00Z");
    private static final Instant ARRIVAL = Instant.parse("2026-09-01T14:00:00Z");
    private static final Instant BEFORE_DEPARTURE = DEPARTURE.minusSeconds(3600);

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

            Assertions.assertThat(flight.isBookable(BEFORE_DEPARTURE)).isTrue();
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
            Flight soldOut = twoSeatsLeft()
                    .block(aBooking(), new SeatCount(2), BEFORE_DEPARTURE)
                    .flight();

            Assertions.assertThat(soldOut.isBookable(BEFORE_DEPARTURE)).isFalse();
        }
    }

    @Nested
    @DisplayName("when blocking seats for a booking")
    class Blocking {

        @Test
        @DisplayName("should hand back the flight with those seats taken off")
        void shouldHandBackTheFlightWithThoseSeatsTakenOff() {
            Flight flight = bogotaToMedellin();

            SeatsBlocked result = flight.block(aBooking(), new SeatCount(2), BEFORE_DEPARTURE);

            Assertions.assertThat(result.flight().seats().available()).isEqualTo(118);
            Assertions.assertThat(flight.seats().available()).isEqualTo(120);
        }

        @Test
        @DisplayName("should tie the block to the flight and the booking that asked")
        void shouldTieTheBlockToTheFlightAndTheBookingThatAsked() {
            Flight flight = bogotaToMedellin();
            BookingId booking = aBooking();

            SeatsBlocked result = flight.block(booking, new SeatCount(2), BEFORE_DEPARTURE);

            Assertions.assertThat(result.block().flightId()).isEqualTo(flight.id());
            Assertions.assertThat(result.block().bookingId()).isEqualTo(booking);
            Assertions.assertThat(result.block().seats().value()).isEqualTo(2);
        }

        @Test
        @DisplayName("should sell the last seats a flight has")
        void shouldSellTheLastSeatsAFlightHas() {
            Flight flight = twoSeatsLeft();

            SeatsBlocked result = flight.block(aBooking(), new SeatCount(2), BEFORE_DEPARTURE);

            Assertions.assertThat(result.flight().seats().available()).isZero();
        }

        @Test
        @DisplayName("should refuse a flight that has already departed")
        void shouldRefuseAFlightThatHasAlreadyDeparted() {
            Flight flight = bogotaToMedellin();
            Instant afterDeparture = DEPARTURE.plusSeconds(60);
            BookingId booking = aBooking();
            SeatCount two = new SeatCount(2);

            Assertions.assertThatThrownBy(() -> flight.block(booking, two, afterDeparture))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("no longer open for booking");
        }

        /*
         * The message matters as much as the refusal: it is what tells the
         * passenger how many seats they could have had. Guarding on departure
         * alone rather than isBookable is what keeps it.
         */
        @Test
        @DisplayName("should refuse more seats than are left, and say how many there were")
        void shouldRefuseMoreSeatsThanAreLeft() {
            Flight flight = twoSeatsLeft();
            BookingId booking = aBooking();
            SeatCount three = new SeatCount(3);

            Assertions.assertThatThrownBy(() -> flight.block(booking, three, BEFORE_DEPARTURE))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("only 2 available");
        }
    }

    @Nested
    @DisplayName("when releasing seats")
    class Releasing {

        @Test
        @DisplayName("should come back to the original state after a compensation")
        void shouldComeBackToTheOriginalStateAfterACompensation() {
            Flight flight = bogotaToMedellin();

            Flight compensated = flight
                    .block(aBooking(), new SeatCount(3), BEFORE_DEPARTURE)
                    .flight()
                    .releaseSeats(3);

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

    /**
     * A flight small enough that a single block can empty it. SeatCount caps a
     * booking at nine seats, so the 120-seat flight above cannot be sold out in
     * one call.
     */
    private static Flight twoSeatsLeft() {
        return Flight.schedule(
                new FlightNumber("AV102"),
                new AirportCode("BOG"),
                new AirportCode("MDE"),
                new FlightSchedule(DEPARTURE, ARRIVAL),
                2,
                Money.of("250000.00", "COP"));
    }

    private static BookingId aBooking() {
        return new BookingId(UUID.randomUUID());
    }
}