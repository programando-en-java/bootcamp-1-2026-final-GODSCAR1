package com.programandoenjava.airline.booking.domain.booking;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;
import com.programandoenjava.airline.booking.domain.shared.Money;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

@DisplayName("Booking")
class BookingTest {

    private static final Instant CREATED_AT = Instant.parse("2026-03-10T12:00:00Z");
    private static final String FARE = "250000.00";
    private static final String COP = "COP";
    private static final int TWO_SEATS = 2;

    @Nested
    @DisplayName("when made against held seats")
    class Made {

        @Test
        @DisplayName("should charge the fare once per seat")
        void shouldChargeTheFareOncePerSeat() {
            Money fare = Money.of(FARE, COP);

            Booking booking = aBookingFor(TWO_SEATS, fare);

            Money expected = Money.of("500000.00", COP);
            Assertions.assertThat(booking.total()).isEqualTo(expected);
        }

        @Test
        @DisplayName("should keep the fare it was sold at alongside the total")
        void shouldKeepTheFareItWasSoldAtAlongsideTheTotal() {
            Money fare = Money.of(FARE, COP);

            Booking booking = aBookingFor(TWO_SEATS, fare);

            Assertions.assertThat(booking.pricePerSeat()).isEqualTo(fare);
        }

        @Test
        @DisplayName("should start out unpaid")
        void shouldStartOutUnpaid() {
            Booking booking = aBookingFor(TWO_SEATS, Money.of(FARE, COP));

            Assertions.assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
        }

        @Test
        @DisplayName("should keep the identity it was given")
        void shouldKeepTheIdentityItWasGiven() {
            BookingId id = BookingId.newId();
            Money fare = Money.of(FARE, COP);
            SeatCount seats = new SeatCount(TWO_SEATS);

            Booking booking = Booking.of(id, aPassenger(), aFlight(), aSeatBlock(),
                    seats, fare, CREATED_AT);

            Assertions.assertThat(booking.id()).isEqualTo(id);
        }

        @Test
        @DisplayName("should record when it was made")
        void shouldRecordWhenItWasMade() {
            Booking booking = aBookingFor(TWO_SEATS, Money.of(FARE, COP));

            Assertions.assertThat(booking.createdAt()).isEqualTo(CREATED_AT);
        }
    }

    @Nested
    @DisplayName("when the total does not match what was sold")
    class Mismatched {

        @Test
        @DisplayName("should refuse a total that undercharges")
        void shouldRefuseATotalThatUndercharges() {
            Money fare = Money.of(FARE, COP);
            Money tooLittle = Money.of("250000.00", COP);

            Assertions.assertThatThrownBy(() -> bookingWith(TWO_SEATS, fare, tooLittle))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("does not match");
        }

        @Test
        @DisplayName("should refuse a total that overcharges")
        void shouldRefuseATotalThatOvercharges() {
            Money fare = Money.of(FARE, COP);
            Money tooMuch = Money.of("750000.00", COP);

            Assertions.assertThatThrownBy(() -> bookingWith(TWO_SEATS, fare, tooMuch))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("does not match");
        }

        @Test
        @DisplayName("should refuse a total in another currency")
        void shouldRefuseATotalInAnotherCurrency() {
            Money fare = Money.of(FARE, COP);
            Money elsewhere = Money.of("500000.00", "USD");

            Assertions.assertThatThrownBy(() -> bookingWith(TWO_SEATS, fare, elsewhere))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    private static Booking aBookingFor(final int seats, final Money fare) {
        return Booking.of(
                BookingId.newId(),
                aPassenger(),
                aFlight(),
                aSeatBlock(),
                new SeatCount(seats),
                fare,
                CREATED_AT);
    }

    private static Booking bookingWith(final int seats, final Money fare, final Money total) {
        return new Booking(
                BookingId.newId(),
                aPassenger(),
                aFlight(),
                aSeatBlock(),
                new SeatCount(seats),
                fare,
                total,
                BookingStatus.PENDING,
                CREATED_AT);
    }

    private static PassengerId aPassenger() {
        UUID id = UUID.randomUUID();

        return new PassengerId(id);
    }

    private static FlightId aFlight() {
        UUID id = UUID.randomUUID();

        return new FlightId(id);
    }

    private static SeatBlockId aSeatBlock() {
        UUID id = UUID.randomUUID();

        return new SeatBlockId(id);
    }
}