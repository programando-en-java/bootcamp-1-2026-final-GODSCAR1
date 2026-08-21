package com.programandoenjava.airline.checkin.domain.boardingpass;

import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

@DisplayName("Boarding pass")
class BoardingPassTest {

    private static final Instant NOW = Instant.parse("2026-03-11T07:30:00Z");
    private static final Instant DEPARTURE = Instant.parse("2026-03-11T08:00:00Z");

    private static final String FLIGHT_NUMBER = "AV8001";
    private static final String ORIGIN = "BOG";
    private static final String DESTINATION = "MDE";

    @Nested
    @DisplayName("when it is issued")
    class Issuing {

        @Test
        @DisplayName("should carry the booking and the passenger it belongs to")
        void shouldCarryTheBookingAndThePassengerItBelongsTo() {
            BookingId booking = aBooking();
            PassengerId passenger = aPassenger();

            BoardingPass pass = BoardingPass.issue(
                    booking, passenger, aFlight(), first(), NOW);

            Assertions.assertThat(pass.bookingId()).isEqualTo(booking);
            Assertions.assertThat(pass.passengerId()).isEqualTo(passenger);
        }

        @Test
        @DisplayName("should record the moment it was issued")
        void shouldRecordTheMomentItWasIssued() {
            BoardingPass pass = BoardingPass.issue(
                    aBooking(), aPassenger(), aFlight(), first(), NOW);

            Assertions.assertThat(pass.issuedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("should give each pass an id of its own")
        void shouldGiveEachPassAnIdOfItsOwn() {
            BookingId booking = aBooking();
            PassengerId passenger = aPassenger();

            BoardingPass one = BoardingPass.issue(booking, passenger, aFlight(), first(), NOW);
            BoardingPass another = BoardingPass.issue(booking, passenger, aFlight(), first(), NOW);

            Assertions.assertThat(one.id()).isNotEqualTo(another.id());
        }
    }

    @Nested
    @DisplayName("what it cannot be without")
    class Required {

        @Test
        @DisplayName("should refuse a pass that names no booking")
        void shouldRefuseAPassThatNamesNoBooking() {
            Assertions.assertThatThrownBy(() -> new BoardingPass(
                    BoardingPassId.newId(), null, aPassenger(), aFlight(), first(), NOW))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("must name a booking");
        }

        @Test
        @DisplayName("should refuse a pass that names no passenger")
        void shouldRefuseAPassThatNamesNoPassenger() {
            Assertions.assertThatThrownBy(() -> new BoardingPass(
                    BoardingPassId.newId(), aBooking(), null, aFlight(), first(), NOW))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("must name a passenger");
        }

        @Test
        @DisplayName("should refuse a pass that names no flight")
        void shouldRefuseAPassThatNamesNoFlight() {
            Assertions.assertThatThrownBy(() -> new BoardingPass(
                    BoardingPassId.newId(), aBooking(), aPassenger(), null, first(), NOW))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("must name a flight");
        }

        @Test
        @DisplayName("should refuse a pass with no place in the boarding order")
        void shouldRefuseAPassWithNoPlaceInTheBoardingOrder() {
            Assertions.assertThatThrownBy(() -> new BoardingPass(
                    BoardingPassId.newId(), aBooking(), aPassenger(), aFlight(), null, NOW))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("boarding sequence");
        }
    }

    @Nested
    @DisplayName("the flight it was printed from")
    class Snapshot {

        @Test
        @DisplayName("should carry the number, the route and the departure")
        void shouldCarryTheNumberTheRouteAndTheDeparture() {
            FlightSnapshot flight = aFlight();

            Assertions.assertThat(flight.flightNumber()).isEqualTo(FLIGHT_NUMBER);
            Assertions.assertThat(flight.origin()).isEqualTo(ORIGIN);
            Assertions.assertThat(flight.destination()).isEqualTo(DESTINATION);
            Assertions.assertThat(flight.departure()).isEqualTo(DEPARTURE);
        }

        @Test
        @DisplayName("should refuse a snapshot with no flight number")
        void shouldRefuseASnapshotWithNoFlightNumber() {
            Assertions.assertThatThrownBy(() -> new FlightSnapshot(
                    aFlightId(), "  ", ORIGIN, DESTINATION, DEPARTURE))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("flight number");
        }

        @Test
        @DisplayName("should refuse a snapshot with half a route")
        void shouldRefuseASnapshotWithHalfARoute() {
            Assertions.assertThatThrownBy(() -> new FlightSnapshot(
                    aFlightId(), FLIGHT_NUMBER, ORIGIN, null, DEPARTURE))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("origin and a destination");
        }

        @Test
        @DisplayName("should refuse a snapshot with no departure time")
        void shouldRefuseASnapshotWithNoDepartureTime() {
            Assertions.assertThatThrownBy(() -> new FlightSnapshot(
                    aFlightId(), FLIGHT_NUMBER, ORIGIN, DESTINATION, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("departure time");
        }
    }

    @Nested
    @DisplayName("the boarding sequence")
    class Sequence {

        @Test
        @DisplayName("should start at one")
        void shouldStartAtOne() {
            BoardingSequence sequence = new BoardingSequence(BoardingSequence.FIRST);

            Assertions.assertThat(sequence.value()).isEqualTo(1);
        }

        @Test
        @DisplayName("should refuse a place before the first")
        void shouldRefuseAPlaceBeforeTheFirst() {
            Assertions.assertThatThrownBy(() -> new BoardingSequence(0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("starts at 1");
        }

        @Test
        @DisplayName("should refuse a negative place")
        void shouldRefuseANegativePlace() {
            Assertions.assertThatThrownBy(() -> new BoardingSequence(-1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("starts at 1");
        }
    }

    private static BookingId aBooking() {
        return new BookingId(UUID.randomUUID());
    }

    private static PassengerId aPassenger() {
        return new PassengerId(UUID.randomUUID());
    }

    private static FlightId aFlightId() {
        return new FlightId(UUID.randomUUID());
    }

    private static FlightSnapshot aFlight() {
        return new FlightSnapshot(aFlightId(), FLIGHT_NUMBER, ORIGIN, DESTINATION, DEPARTURE);
    }

    private static BoardingSequence first() {
        return new BoardingSequence(BoardingSequence.FIRST);
    }
}
