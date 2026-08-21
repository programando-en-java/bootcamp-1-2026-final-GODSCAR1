package com.programandoenjava.airline.e2e;

import com.jayway.jsonpath.JsonPath;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@EnabledIfSystemProperty(named = "airline.e2e", matches = "true")
@DisplayName("Telling the passenger, end to end")
class NotificationJourneyE2ETest {

    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String PAYMENTS = "/api/v1/payments";
    private static final String BOARDING_PASSES = "/api/v1/boarding-passes";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String GOOD_CARD = "4242424242424242";
    private static final String DECLINING_CARD = "4000000000000002";

    private static final int SEATS_WANTED = 1;
    private static final int CAPACITY = 120;
    private static final String FARE = "250000.00";
    private static final String COP = "COP";

    private static final String PENDING = "PENDING";

    private static final String BOOKING_CREATED = "BOOKING_CREATED";
    private static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    private static final String CHECK_IN_COMPLETED = "CHECK_IN_COMPLETED";

    private static final int HOURS_UNTIL_DEPARTURE = 3;

    private static final Duration ARRIVAL_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration BETWEEN_POLLS = Duration.ofMillis(250);

    private static final String INSERT_FLIGHT = """
            INSERT INTO flights (id, flight_number, origin, destination,
                                 departure_time, arrival_time,
                                 total_seats, available_seats,
                                 price_amount, price_currency)
            VALUES (?, ?, 'BOG', 'MDE', ?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_BOOKING_STATUS =
            "SELECT status FROM bookings WHERE id = ?";
    private static final String FIND_TYPES_FOR = """
            SELECT type FROM notifications WHERE booking_id = ? ORDER BY created_at
            """;
    private static final String COUNT_OF_TYPE_FOR =
            "SELECT count(*) FROM notifications WHERE booking_id = ? AND type = ?";
    private static final String COUNT_UNSENT_FOR =
            "SELECT count(*) FROM notifications WHERE booking_id = ? AND sent_at IS NULL";
    private static final String FIND_BODY_FOR =
            "SELECT body FROM notifications WHERE booking_id = ? AND type = ?";

    private static final AtomicInteger FLIGHT_SEQUENCE = new AtomicInteger();

    private static RestClient bookingService;
    private static RestClient paymentService;
    private static RestClient checkinService;
    private static JdbcTemplate flightDatabase;
    private static JdbcTemplate bookingDatabase;
    private static JdbcTemplate notificationDatabase;

    @BeforeAll
    static void connect() {
        AirlineStack.start();

        bookingService = client(AirlineStack.bookingServiceUrl());
        paymentService = client(AirlineStack.paymentServiceUrl());
        checkinService = client(AirlineStack.checkinServiceUrl());

        flightDatabase = database(AirlineStack.flightDatabaseUrl());
        bookingDatabase = database(AirlineStack.bookingDatabaseUrl());
        notificationDatabase = database(AirlineStack.notificationDatabaseUrl());
    }

    @Nested
    @DisplayName("when a booking is made")
    class BookingCreated {

        @Test
        @DisplayName("should tell the passenger, without anyone calling notification-service")
        void shouldTellThePassenger() {
            UUID flightId = aFlight();
            String bookingId = aBookingOn(flightId);

            awaitNotification(bookingId, BOOKING_CREATED);
        }

        @Test
        @DisplayName("should say what is held and what is owed")
        void shouldSayWhatIsHeldAndWhatIsOwed() {
            UUID flightId = aFlight();
            String bookingId = aBookingOn(flightId);

            awaitNotification(bookingId, BOOKING_CREATED);

            String body = bodyOf(bookingId, BOOKING_CREATED);

            Assertions.assertThat(body).contains(FARE);
        }
    }

    @Nested
    @DisplayName("when the payment goes through")
    class PaymentSucceeded {

        @Test
        @DisplayName("should tell the passenger their payment arrived")
        void shouldTellThePassengerTheirPaymentArrived() {
            UUID flightId = aFlight();
            String bookingId = aBookingOn(flightId);

            pay(bookingId, GOOD_CARD);
            awaitSettled(bookingId);

            awaitNotification(bookingId, PAYMENT_SUCCEEDED);
        }

        @Test
        @DisplayName("should say nothing when the card is declined")
        void shouldSayNothingWhenTheCardIsDeclined() {
            UUID flightId = aFlight();
            String bookingId = aBookingOn(flightId);

            awaitNotification(bookingId, BOOKING_CREATED);

            pay(bookingId, DECLINING_CARD);
            awaitSettled(bookingId);

            Assertions.assertThat(countOf(bookingId, PAYMENT_SUCCEEDED)).isZero();
        }
    }

    @Nested
    @DisplayName("when the passenger checks in")
    class CheckInCompleted {

        @Test
        @DisplayName("should tell them, naming the flight")
        void shouldTellThemNamingTheFlight() {
            String bookingId = aCheckedInBooking();

            awaitNotification(bookingId, CHECK_IN_COMPLETED);

            String body = bodyOf(bookingId, CHECK_IN_COMPLETED);

            Assertions.assertThat(body)
                    .contains("BOG")
                    .contains("MDE");
        }
    }

    @Nested
    @DisplayName("across the whole journey")
    class Whole {

        @Test
        @DisplayName("should tell the passenger three times, once for each thing that happened")
        void shouldTellThePassengerThreeTimes() {
            String bookingId = aCheckedInBooking();

            awaitAllThree(bookingId);

            List<String> types = typesFor(bookingId);

            Assertions.assertThat(types)
                    /* In any order: three topics, and Kafka orders within a partition, not across them. */
                    .containsExactlyInAnyOrder(
                            BOOKING_CREATED, PAYMENT_SUCCEEDED, CHECK_IN_COMPLETED);
        }

        @Test
        @DisplayName("should have got all three out through the channel")
        void shouldHaveGotAllThreeOutThroughTheChannel() {
            String bookingId = aCheckedInBooking();

            awaitAllThree(bookingId);

            await(() -> unsentFor(bookingId) == 0);
        }
    }

    private String aCheckedInBooking() {
        UUID flightId = aFlight();
        String bookingId = aBookingOn(flightId);

        pay(bookingId, GOOD_CARD);
        awaitSettled(bookingId);

        checkIn(bookingId);

        return bookingId;
    }

    private UUID aFlight() {
        UUID id = UUID.randomUUID();
        int sequence = FLIGHT_SEQUENCE.incrementAndGet();
        String flightNumber = "NT%04d".formatted(sequence);
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC)
                .plusHours(HOURS_UNTIL_DEPARTURE);
        OffsetDateTime arrival = departure.plusHours(2);
        BigDecimal fare = new BigDecimal(FARE);

        flightDatabase.update(INSERT_FLIGHT,
                id, flightNumber, departure, arrival, CAPACITY, CAPACITY, fare, COP);

        return id;
    }

    private String aBookingOn(final UUID flightId) {
        String body = """
                {"passengerId": "%s", "flightId": "%s", "seats": %d}
                """.formatted(UUID.randomUUID(), flightId, SEATS_WANTED);

        ResponseEntity<String> response = bookingService.post()
                .uri(BOOKINGS)
                .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        Assertions.assertThat(response.getStatusCode().value())
                .as("creating a booking")
                .isEqualTo(HttpStatus.CREATED.value());

        return JsonPath.read(response.getBody(), "$.bookingId");
    }

    private void pay(final String bookingId, final String cardNumber) {
        String body = """
                {"bookingId": "%s", "cardNumber": "%s"}
                """.formatted(bookingId, cardNumber);

        paymentService.post()
                .uri(PAYMENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
    }

    private void checkIn(final String bookingId) {
        String body = """
                {"bookingId": "%s"}
                """.formatted(bookingId);

        ResponseEntity<String> response = checkinService.post()
                .uri(BOARDING_PASSES)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        Assertions.assertThat(response.getStatusCode().value())
                .as("checking in")
                .isEqualTo(HttpStatus.CREATED.value());
    }

    private void awaitSettled(final String bookingId) {
        await(() -> !PENDING.equals(statusOf(bookingId)));
    }

    private void awaitNotification(final String bookingId, final String type) {
        await(() -> countOf(bookingId, type) == 1);
    }

    private void awaitAllThree(final String bookingId) {
        awaitNotification(bookingId, BOOKING_CREATED);
        awaitNotification(bookingId, PAYMENT_SUCCEEDED);
        awaitNotification(bookingId, CHECK_IN_COMPLETED);
    }

    private static void await(final Supplier<Boolean> condition) {
        Instant deadline = Instant.now().plus(ARRIVAL_TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            if (condition.get()) {
                return;
            }
            sleep();
        }

        throw new AssertionError("Nothing happened within " + ARRIVAL_TIMEOUT);
    }

    private static void sleep() {
        try {
            Thread.sleep(BETWEEN_POLLS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting", interrupted);
        }
    }

    private String statusOf(final String bookingId) {
        return bookingDatabase.queryForObject(
                FIND_BOOKING_STATUS, String.class, UUID.fromString(bookingId));
    }

    private long countOf(final String bookingId, final String type) {
        return notificationDatabase.queryForObject(
                COUNT_OF_TYPE_FOR, Long.class, UUID.fromString(bookingId), type);
    }

    private long unsentFor(final String bookingId) {
        return notificationDatabase.queryForObject(
                COUNT_UNSENT_FOR, Long.class, UUID.fromString(bookingId));
    }

    private List<String> typesFor(final String bookingId) {
        return notificationDatabase.queryForList(
                FIND_TYPES_FOR, String.class, UUID.fromString(bookingId));
    }

    private String bodyOf(final String bookingId, final String type) {
        return notificationDatabase.queryForObject(
                FIND_BODY_FOR, String.class, UUID.fromString(bookingId), type);
    }

    private static RestClient client(final String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                })
                .build();
    }

    private static JdbcTemplate database(final String url) {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(url);
        dataSource.setUsername(AirlineStack.databaseUser());
        dataSource.setPassword(AirlineStack.databasePassword());

        return new JdbcTemplate(dataSource);
    }
}
