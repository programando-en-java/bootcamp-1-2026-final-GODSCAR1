package com.programandoenjava.airline.booking.infrastructure.adapter.in.events;

import com.programandoenjava.airline.booking.EnableDatabaseTest;
import com.programandoenjava.airline.booking.TestcontainersConfiguration;
import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingCommand;
import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.ConfirmBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.FailBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.SettleBookingCommand;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.SeatsHeld;
import com.programandoenjava.airline.booking.application.port.out.releaseseats.ReleaseSeatsPort;
import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.domain.booking.BookingId;
import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.PassengerId;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import com.programandoenjava.airline.booking.domain.booking.SeatCount;
import com.programandoenjava.airline.booking.domain.shared.Money;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking.BookingPersistenceConfiguration;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.processedevents.ProcessedEventsConfiguration;
import com.programandoenjava.airline.booking.infrastructure.config.ApplicationConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;

/**
 * What happens when a payment event arrives, against a real database.
 *
 * <p>No Kafka and no listener: those carry a message from one place to another,
 * and the use cases are where the deciding happens. flight-service is mocked
 * because a released hold is its business, not this one's — that the two agree
 * is what the end-to-end test is for.
 */
@SpringBootTest(classes = {
        ApplicationConfiguration.class,
        BookingPersistenceConfiguration.class,
        ProcessedEventsConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@Sql(scripts = "/db/testdata/R__reset_bookings.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Settling a booking (hand-built, no full context)")
class SettleBookingSliceTest {

    private static final String CREATED_AT = "2026-03-10T12:00:00Z";
    private static final String RELEASED_AT = "2026-03-10T12:05:00Z";

    private static final String FARE = "250000.00";
    private static final String COP = "COP";
    private static final int SEATS_WANTED = 2;

    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String FAILED = "FAILED";

    private static final String FIND_STATUS = "SELECT status FROM bookings WHERE id = ?";
    private static final String FIND_RELEASED_AT =
            "SELECT seats_released_at FROM bookings WHERE id = ?";
    private static final String COUNT_PROCESSED = "SELECT count(*) FROM processed_events";

    @Autowired
    private CreateBookingUseCase createBooking;

    @Autowired
    private ConfirmBookingUseCase confirmBooking;

    @Autowired
    private FailBookingUseCase failBooking;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private HoldSeatsPort holdSeatsPort;

    @MockitoBean
    private ReleaseSeatsPort releaseSeatsPort;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse(RELEASED_AT), ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("when the payment succeeded")
    class Confirming {

        @Test
        @DisplayName("should confirm the booking")
        void shouldConfirmTheBooking() {
            Booking booking = aPendingBooking();

            confirmBooking.confirm(anEventFor(booking));

            Assertions.assertThat(statusOf(booking)).isEqualTo(CONFIRMED);
        }

        @Test
        @DisplayName("should leave the seats where they are")
        void shouldLeaveTheSeatsWhereTheyAre() {
            Booking booking = aPendingBooking();

            confirmBooking.confirm(anEventFor(booking));

            Mockito.verifyNoInteractions(releaseSeatsPort);
            Assertions.assertThat(releasedAt(booking)).isNull();
        }

        /*
         * The delivery guarantee is at-least-once, so this is the ordinary case
         * rather than an edge one.
         */
        @Test
        @DisplayName("should do nothing the second time the same event arrives")
        void shouldDoNothingTheSecondTimeTheSameEventArrives() {
            Booking booking = aPendingBooking();
            SettleBookingCommand event = anEventFor(booking);

            confirmBooking.confirm(event);
            confirmBooking.confirm(event);

            Assertions.assertThat(statusOf(booking)).isEqualTo(CONFIRMED);
            Assertions.assertThat(processedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should raise for a booking that does not exist")
        void shouldRaiseForABookingThatDoesNotExist() {
            BookingId unknown = BookingId.newId();
            SettleBookingCommand event = new SettleBookingCommand(UUID.randomUUID(), unknown);

            Assertions.assertThatThrownBy(() -> confirmBooking.confirm(event))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("when the payment failed")
    class Failing {

        @Test
        @DisplayName("should fail the booking")
        void shouldFailTheBooking() {
            Booking booking = aPendingBooking();

            failBooking.fail(anEventFor(booking));

            Assertions.assertThat(statusOf(booking)).isEqualTo(FAILED);
        }

        /*
         * US-006. The hold is named by the booking, which is why the
         * compensation lives here rather than in payment-service.
         */
        @Test
        @DisplayName("should give the seats back to the flight they were held on")
        void shouldGiveTheSeatsBackToTheFlightTheyWereHeldOn() {
            Booking booking = aPendingBooking();

            failBooking.fail(anEventFor(booking));

            Mockito.verify(releaseSeatsPort)
                    .release(booking.flightId(), booking.seatBlockId());
        }

        @Test
        @DisplayName("should record when the seats went back")
        void shouldRecordWhenTheSeatsWentBack() {
            Booking booking = aPendingBooking();

            failBooking.fail(anEventFor(booking));

            Assertions.assertThat(releasedAt(booking)).isEqualTo(Instant.parse(RELEASED_AT));
        }

        @Test
        @DisplayName("should do nothing the second time the same event arrives")
        void shouldDoNothingTheSecondTimeTheSameEventArrives() {
            Booking booking = aPendingBooking();
            SettleBookingCommand event = anEventFor(booking);

            failBooking.fail(event);
            failBooking.fail(event);

            Mockito.verify(releaseSeatsPort, Mockito.times(1))
                    .release(BDDMockito.any(), BDDMockito.any());
            Assertions.assertThat(processedCount()).isEqualTo(1);
        }

        /*
         * The order the use case works in: mark, then release. A release that
         * throws leaves a booking that says FAILED with its seats still held,
         * which the partial index on seats_released_at exists to find. Releasing
         * first would leave a PENDING booking with no seats, which nothing could
         * tell apart from one still waiting.
         */
        @Test
        @DisplayName("should keep the booking failed even when the seats cannot go back")
        void shouldKeepTheBookingFailedEvenWhenTheSeatsCannotGoBack() {
            Booking booking = aPendingBooking();
            givenFlightServiceIsUnreachable();

            Assertions.assertThatThrownBy(() -> failBooking.fail(anEventFor(booking)))
                    .isInstanceOf(RuntimeException.class);

            Assertions.assertThat(statusOf(booking)).isEqualTo(FAILED);
            Assertions.assertThat(releasedAt(booking)).isNull();
        }
    }

    @Nested
    @DisplayName("when the two events race")
    class Contradicting {

        @Test
        @DisplayName("should refuse to fail a booking that was confirmed")
        void shouldRefuseToFailABookingThatWasConfirmed() {
            Booking booking = aPendingBooking();
            confirmBooking.confirm(anEventFor(booking));

            Assertions.assertThatThrownBy(() -> failBooking.fail(anEventFor(booking)))
                    .isInstanceOf(RuntimeException.class);

            Assertions.assertThat(statusOf(booking)).isEqualTo(CONFIRMED);
        }

        @Test
        @DisplayName("should refuse to confirm a booking that failed")
        void shouldRefuseToConfirmABookingThatFailed() {
            Booking booking = aPendingBooking();
            failBooking.fail(anEventFor(booking));

            Assertions.assertThatThrownBy(() -> confirmBooking.confirm(anEventFor(booking)))
                    .isInstanceOf(RuntimeException.class);

            Assertions.assertThat(statusOf(booking)).isEqualTo(FAILED);
        }
    }

    private Booking aPendingBooking() {
        givenSeatsAreHeld();

        PassengerId passenger = new PassengerId(UUID.randomUUID());
        FlightId flight = new FlightId(UUID.randomUUID());
        SeatCount seats = new SeatCount(SEATS_WANTED);
        IdempotencyKey key = new IdempotencyKey(UUID.randomUUID().toString());
        CreateBookingCommand command =
                new CreateBookingCommand(passenger, flight, seats, key);

        Booking booking = createBooking.create(command);

        Assertions.assertThat(statusOf(booking)).isEqualTo(PENDING);

        return booking;
    }

    private void givenSeatsAreHeld() {
        Currency currency = Currency.getInstance(COP);
        Money fare = new Money(new BigDecimal(FARE), currency);

        BDDMockito.given(holdSeatsPort.hold(BDDMockito.any()))
                .willAnswer(invocation -> {
                    SeatBlockId seatBlockId = new SeatBlockId(UUID.randomUUID());
                    return new SeatsHeld(seatBlockId, fare);
                });
    }

    private void givenFlightServiceIsUnreachable() {
        BDDMockito.willThrow(new IllegalStateException("flight-service did not answer"))
                .given(releaseSeatsPort).release(BDDMockito.any(), BDDMockito.any());
    }

    private static SettleBookingCommand anEventFor(final Booking booking) {
        UUID eventId = UUID.randomUUID();

        return new SettleBookingCommand(eventId, booking.id());
    }

    private String statusOf(final Booking booking) {
        return jdbcTemplate.queryForObject(FIND_STATUS, String.class, booking.id().value());
    }

    private Instant releasedAt(final Booking booking) {
        java.sql.Timestamp at = jdbcTemplate.queryForObject(
                FIND_RELEASED_AT, java.sql.Timestamp.class, booking.id().value());

        return at == null ? null : at.toInstant();
    }

    private long processedCount() {
        return jdbcTemplate.queryForObject(COUNT_PROCESSED, Long.class);
    }
}
