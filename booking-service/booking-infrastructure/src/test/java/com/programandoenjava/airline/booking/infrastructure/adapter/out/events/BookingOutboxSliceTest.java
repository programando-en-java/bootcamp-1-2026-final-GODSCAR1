package com.programandoenjava.airline.booking.infrastructure.adapter.out.events;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.booking.EnableDatabaseTest;
import com.programandoenjava.airline.booking.TestcontainersConfiguration;
import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingCommand;
import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingUseCase;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.SeatsHeld;
import com.programandoenjava.airline.booking.application.port.out.processedevents.ProcessedEventsPort;
import com.programandoenjava.airline.booking.application.port.out.releaseseats.ReleaseSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.PassengerId;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import com.programandoenjava.airline.booking.domain.booking.SeatCount;
import com.programandoenjava.airline.booking.domain.shared.Money;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking.BookingPersistenceConfiguration;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.outbox.OutboxConfiguration;
import com.programandoenjava.airline.booking.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.booking.infrastructure.transaction.TransactionSupportConfiguration;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ADR-001 said booking-service would never need an outbox, because everything
 * it could announce another service already knew. Notifications are the case
 * that breaks it: nobody but booking-service can say that a booking was made.
 *
 * <p>Kafka is absent on purpose. Sending is the relay's job and has its own
 * test; this one is about the row, which is what a crash right after the insert
 * would otherwise lose.
 */
@SpringBootTest(classes = {
        ApplicationConfiguration.class,
        BookingPersistenceConfiguration.class,
        OutboxConfiguration.class,
        EventsConfiguration.class,
        TransactionSupportConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Sql(scripts = "/db/testdata/R__reset_bookings.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Booking outbox (hand-built, no full context)")
class BookingOutboxSliceTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");

    private static final String CREATED_TOPIC = "booking.created.v1";
    private static final String AGGREGATE_TYPE = "booking";

    private static final String COP = "COP";
    private static final String FARE = "250000.00";
    private static final int SEATS = 2;
    private static final double TOTAL_FOR_TWO = 500_000.00;
    private static final double A_CENT = 0.001;

    private static final String PENDING = "PENDING";

    private static final String FIND_MESSAGES = """
            SELECT topic, aggregate_type, aggregate_id, payload, published_at
            FROM outbox ORDER BY created_at
            """;
    private static final String COUNT_MESSAGES = "SELECT count(*) FROM outbox";
    private static final String COUNT_BOOKINGS = "SELECT count(*) FROM bookings";

    @Autowired
    private CreateBookingUseCase createBookingUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private HoldSeatsPort holdSeatsPort;

    /*
     * ApplicationConfiguration wires every use case booking-service has, and
     * two of them settle a booking rather than create one. They are here so the
     * wiring stands up; nothing in this file goes near them.
     */
    @MockitoBean
    private ProcessedEventsPort processedEventsPort;

    @MockitoBean
    private ReleaseSeatsPort releaseSeatsPort;

    /*
     * A spy rather than a mock: the write has to work for most tests, and be
     * made to fail for the one that checks the two writes share a transaction.
     */
    @MockitoSpyBean
    private SaveBookingPort saveBookingPort;

    /*
     * The relay is a bean of OutboxConfiguration and cannot be built without a
     * template. Nothing here calls it: sending has its own test, and what this
     * one is about is the row the listener leaves behind.
     */
    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("when a booking is made")
    class Created {

        @Test
        @DisplayName("should leave one message behind")
        void shouldLeaveOneMessageBehind() {
            givenSeatsAreHeld();

            createBooking();

            Assertions.assertThat(messageCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should announce it on the created topic")
        void shouldAnnounceItOnTheCreatedTopic() {
            givenSeatsAreHeld();

            createBooking();

            Map<String, Object> message = theOnlyMessage();

            Assertions.assertThat(message.get("topic")).isEqualTo(CREATED_TOPIC);
            Assertions.assertThat(message.get("aggregate_type")).isEqualTo(AGGREGATE_TYPE);
        }

        @Test
        @DisplayName("should key the message by the booking it is about")
        void shouldKeyTheMessageByTheBookingItIsAbout() {
            givenSeatsAreHeld();

            String bookingId = createBooking();

            Assertions.assertThat(theOnlyMessage().get("aggregate_id")).isEqualTo(bookingId);
        }

        @Test
        @DisplayName("should leave it unsent for the relay to pick up")
        void shouldLeaveItUnsentForTheRelayToPickUp() {
            givenSeatsAreHeld();

            createBooking();

            Assertions.assertThat(theOnlyMessage().get("published_at")).isNull();
        }
    }

    @Nested
    @DisplayName("what the message carries")
    class Payload {

        @Test
        @DisplayName("should carry an event id, so a consumer can recognise a repeat")
        void shouldCarryAnEventId() {
            givenSeatsAreHeld();

            createBooking();

            String eventId = JsonPath.read(payloadOfTheOnlyMessage(), "$.eventId");

            Assertions.assertThat(eventId).isNotBlank();
        }

        /*
         * The passenger is here because a notification has to be addressed to
         * someone, and this is the only message in the system that says who a
         * booking belongs to.
         */
        @Test
        @DisplayName("should name the passenger the booking belongs to")
        void shouldNameThePassengerTheBookingBelongsTo() {
            givenSeatsAreHeld();

            UUID passenger = UUID.randomUUID();
            createBookingFor(passenger);

            String named = JsonPath.read(payloadOfTheOnlyMessage(), "$.passengerId");

            Assertions.assertThat(named).isEqualTo(passenger.toString());
        }

        @Test
        @DisplayName("should carry what was booked and what it costs")
        void shouldCarryWhatWasBookedAndWhatItCosts() {
            givenSeatsAreHeld();

            createBooking();

            String payload = payloadOfTheOnlyMessage();

            int seats = JsonPath.read(payload, "$.seats");
            double total = ((Number) JsonPath.read(payload, "$.total")).doubleValue();
            String currency = JsonPath.read(payload, "$.currency");
            String status = JsonPath.read(payload, "$.status");

            Assertions.assertThat(seats).isEqualTo(SEATS);
            Assertions.assertThat(total).isCloseTo(TOTAL_FOR_TWO, Assertions.within(A_CENT));
            Assertions.assertThat(currency).isEqualTo(COP);
            Assertions.assertThat(status).isEqualTo(PENDING);
        }
    }

    @Nested
    @DisplayName("when the same request arrives twice")
    class Repeated {

        /*
         * The second request carries the key the first one took, so no booking
         * is written and nothing new has happened to announce. Announcing on
         * every request would notify a passenger each time a retry went through.
         */
        @Test
        @DisplayName("should announce nothing the second time")
        void shouldAnnounceNothingTheSecondTime() {
            givenSeatsAreHeld();

            IdempotencyKey key = new IdempotencyKey(UUID.randomUUID().toString());
            UUID passenger = UUID.randomUUID();
            UUID flight = UUID.randomUUID();

            createBooking(passenger, flight, key);
            createBooking(passenger, flight, key);

            Assertions.assertThat(messageCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("when the booking cannot be written")
    class Rollback {

        /*
         * The reason the listener runs BEFORE_COMMIT. In separate transactions
         * this would announce a booking that does not exist, and a passenger
         * would be told about something they cannot pay for.
         */
        @Test
        @DisplayName("should announce nothing")
        void shouldAnnounceNothing() {
            givenSeatsAreHeld();
            givenTheWriteFails();

            Assertions.assertThatThrownBy(this::createBookingThatFails)
                    .isInstanceOf(RuntimeException.class);

            Assertions.assertThat(messageCount()).isZero();
            Assertions.assertThat(bookingCount()).isZero();
        }

        private void createBookingThatFails() {
            createBooking();
        }
    }

    private void givenSeatsAreHeld() {
        SeatsHeld held = new SeatsHeld(
                new SeatBlockId(UUID.randomUUID()),
                new Money(new BigDecimal(FARE), Currency.getInstance(COP)));

        BDDMockito.given(holdSeatsPort.hold(BDDMockito.any())).willReturn(held);
    }

    private void givenTheWriteFails() {
        BDDMockito.willThrow(new IllegalStateException("the database said no"))
                .given(saveBookingPort).saveIfNew(BDDMockito.any(), BDDMockito.any());
    }

    private String createBooking() {
        return createBookingFor(UUID.randomUUID());
    }

    private String createBookingFor(final UUID passengerId) {
        IdempotencyKey key = new IdempotencyKey(UUID.randomUUID().toString());

        return createBooking(passengerId, UUID.randomUUID(), key);
    }

    private String createBooking(final UUID passengerId,
                                 final UUID flightId,
                                 final IdempotencyKey key) {

        CreateBookingCommand command = new CreateBookingCommand(
                new PassengerId(passengerId),
                new FlightId(flightId),
                new SeatCount(SEATS),
                key);

        return createBookingUseCase.create(command).id().value().toString();
    }

    private Map<String, Object> theOnlyMessage() {
        List<Map<String, Object>> messages = jdbcTemplate.queryForList(FIND_MESSAGES);

        Assertions.assertThat(messages).hasSize(1);

        return messages.getFirst();
    }

    private String payloadOfTheOnlyMessage() {
        return String.valueOf(theOnlyMessage().get("payload"));
    }

    private long messageCount() {
        return jdbcTemplate.queryForObject(COUNT_MESSAGES, Long.class);
    }

    private long bookingCount() {
        return jdbcTemplate.queryForObject(COUNT_BOOKINGS, Long.class);
    }
}
