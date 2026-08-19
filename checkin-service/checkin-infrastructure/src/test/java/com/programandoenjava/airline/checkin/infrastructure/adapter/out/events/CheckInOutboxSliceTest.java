package com.programandoenjava.airline.checkin.infrastructure.adapter.out.events;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.checkin.EnableDatabaseTest;
import com.programandoenjava.airline.checkin.TestcontainersConfiguration;
import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInCommand;
import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInUseCase;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.SaveBoardingPassPort;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.BookingToCheckIn;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.checkin.application.port.out.readflight.ReadFlightPort;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
import com.programandoenjava.airline.checkin.domain.boardingpass.PassengerId;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass.BoardingPassPersistenceConfiguration;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence.BoardingSequenceConfiguration;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox.OutboxConfiguration;
import com.programandoenjava.airline.checkin.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.checkin.infrastructure.transaction.TransactionSupportConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the check-in slice cannot say, because it mocks the publisher: that
 * issuing a pass leaves a message behind, and that the message shares the
 * pass's transaction.
 *
 * <p>Kafka is absent on purpose. Sending is the relay's job and has its own
 * test; this one is about the row, which is what a crash after printing would
 * otherwise lose.
 */
@SpringBootTest(classes = {
        ApplicationConfiguration.class,
        BoardingPassPersistenceConfiguration.class,
        BoardingSequenceConfiguration.class,
        OutboxConfiguration.class,
        EventsConfiguration.class,
        TransactionSupportConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Sql(scripts = "/db/testdata/R__reset_boarding_passes.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Check-in outbox (hand-built, no full context)")
class CheckInOutboxSliceTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");
    private static final Instant SOON = NOW.plus(Duration.ofHours(20));

    private static final String FLIGHT_NUMBER = "AV8001";
    private static final String ORIGIN = "BOG";
    private static final String DESTINATION = "MDE";
    private static final String CONFIRMED = "CONFIRMED";

    private static final String COMPLETED_TOPIC = "checkin.completed.v1";
    private static final String AGGREGATE_TYPE = "boarding_pass";

    private static final String FIND_MESSAGES = """
            SELECT topic, aggregate_type, aggregate_id, payload, published_at
            FROM outbox ORDER BY created_at
            """;
    private static final String COUNT_MESSAGES = "SELECT count(*) FROM outbox";
    private static final String COUNT_PASSES = "SELECT count(*) FROM boarding_passes";

    @Autowired
    private CheckInUseCase checkInUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReadBookingPort readBookingPort;

    @MockitoBean
    private ReadFlightPort readFlightPort;

    /*
     * A spy rather than a mock: the write has to work for most tests, and be
     * made to fail for the one that checks the two writes share a transaction.
     */
    @MockitoSpyBean
    private SaveBoardingPassPort saveBoardingPassPort;

    /*
     * The relay is a bean of OutboxConfiguration and cannot be built without a
     * template. Nothing here calls it: sending is its own concern, with its own
     * test, and what this one is about is the row the listener leaves behind.
     */
    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("when a passenger checks in")
    class CheckedIn {

        @Test
        @DisplayName("should leave one message behind")
        void shouldLeaveOneMessageBehind() {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight());

            checkIn(booking);

            Assertions.assertThat(messageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should announce it on the completed topic")
        void shouldAnnounceItOnTheCompletedTopic() {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight());

            checkIn(booking);

            Assertions.assertThat(topicOfTheOnlyMessage()).isEqualTo(COMPLETED_TOPIC);
        }

        /*
         * Keyed by the booking rather than by the pass, so everything this
         * system says about one journey lands on one partition and stays in the
         * order it happened.
         */
        @Test
        @DisplayName("should key the message by the booking it is about")
        void shouldKeyTheMessageByTheBookingItIsAbout() {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight());

            checkIn(booking);

            Map<String, Object> message = theOnlyMessage();

            Assertions.assertThat(message.get("aggregate_id")).isEqualTo(booking.toString());
            Assertions.assertThat(message.get("aggregate_type")).isEqualTo(AGGREGATE_TYPE);
        }

        @Test
        @DisplayName("should leave it unsent for the relay to pick up")
        void shouldLeaveItUnsentForTheRelayToPickUp() {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight());

            checkIn(booking);

            Assertions.assertThat(theOnlyMessage().get("published_at")).isNull();
        }
    }

    @Nested
    @DisplayName("what the message carries")
    class Payload {

        @Test
        @DisplayName("should carry an event id, so a consumer can recognise a repeat")
        void shouldCarryAnEventId() {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight());

            checkIn(booking);

            String eventId = JsonPath.read(payloadOfTheOnlyMessage(), "$.eventId");

            Assertions.assertThat(eventId).isNotBlank();
        }

        /*
         * The flight travels in full. A notification written from this must not
         * have to call flight-service to find out which aeroplane it is about.
         */
        @Test
        @DisplayName("should carry enough to write a notification from")
        void shouldCarryEnoughToWriteANotificationFrom() {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight());

            checkIn(booking);

            String payload = payloadOfTheOnlyMessage();

            String bookingId = JsonPath.read(payload, "$.bookingId");
            String flightNumber = JsonPath.read(payload, "$.flightNumber");
            String origin = JsonPath.read(payload, "$.origin");
            String destination = JsonPath.read(payload, "$.destination");
            int sequence = JsonPath.read(payload, "$.boardingSequence");

            Assertions.assertThat(bookingId).isEqualTo(booking.toString());
            Assertions.assertThat(flightNumber).isEqualTo(FLIGHT_NUMBER);
            Assertions.assertThat(origin).isEqualTo(ORIGIN);
            Assertions.assertThat(destination).isEqualTo(DESTINATION);
            Assertions.assertThat(sequence).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("when the same booking checks in again")
    class Repeated {

        /*
         * The pass is not printed a second time, so nothing new happened and
         * nothing is announced. Announcing on every request would have a
         * passenger refreshing the page send a notification each time.
         */
        @Test
        @DisplayName("should announce nothing the second time")
        void shouldAnnounceNothingTheSecondTime() {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight());

            checkIn(booking);
            checkIn(booking);

            Assertions.assertThat(messageCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("when the pass cannot be written")
    class Rollback {

        /*
         * The reason the listener runs BEFORE_COMMIT. If the two writes were in
         * separate transactions, this would announce a check-in that never
         * happened, and nothing downstream could tell the difference.
         */
        @Test
        @DisplayName("should announce nothing")
        void shouldAnnounceNothing() {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight());
            givenTheWriteFails();

            Assertions.assertThatThrownBy(() -> checkIn(booking))
                    .isInstanceOf(RuntimeException.class);

            Assertions.assertThat(messageCount()).isZero();
            Assertions.assertThat(passCount()).isZero();
        }
    }

    private void checkIn(final UUID bookingId) {
        BookingId id = new BookingId(bookingId);
        CheckInCommand command = new CheckInCommand(id);

        checkInUseCase.checkIn(command);
    }

    private void givenBookingIsConfirmed(final UUID bookingId, final UUID flightId) {
        BookingId booking = new BookingId(bookingId);
        FlightId flight = new FlightId(flightId);
        PassengerId passenger = new PassengerId(UUID.randomUUID());

        BookingToCheckIn answer = new BookingToCheckIn(booking, passenger, flight, CONFIRMED);
        BDDMockito.given(readBookingPort.byId(booking)).willReturn(answer);

        FlightSnapshot snapshot = new FlightSnapshot(
                flight, FLIGHT_NUMBER, ORIGIN, DESTINATION, SOON);
        BDDMockito.given(readFlightPort.byId(flight)).willReturn(snapshot);
    }

    private void givenTheWriteFails() {
        BDDMockito.willThrow(new IllegalStateException("the database said no"))
                .given(saveBoardingPassPort).saveIfNew(BDDMockito.any());
    }

    private Map<String, Object> theOnlyMessage() {
        List<Map<String, Object>> messages = jdbcTemplate.queryForList(FIND_MESSAGES);

        Assertions.assertThat(messages).hasSize(1);

        return messages.getFirst();
    }

    private String topicOfTheOnlyMessage() {
        return String.valueOf(theOnlyMessage().get("topic"));
    }

    private String payloadOfTheOnlyMessage() {
        return String.valueOf(theOnlyMessage().get("payload"));
    }

    private long messageCount() {
        return jdbcTemplate.queryForObject(COUNT_MESSAGES, Long.class);
    }

    private long passCount() {
        return jdbcTemplate.queryForObject(COUNT_PASSES, Long.class);
    }

    private static UUID aBooking() {
        return UUID.randomUUID();
    }

    private static UUID aFlight() {
        return UUID.randomUUID();
    }
}
