package com.programandoenjava.airline.e2e;

import com.jayway.jsonpath.JsonPath;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.HttpHeaders;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@EnabledIfSystemProperty(named = "airline.e2e", matches = "true")
@DisplayName("Checking in, end to end")
class CheckInJourneyE2ETest {

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
    private static final String CONFIRMED = "CONFIRMED";

    private static final String NOT_OPEN_YET = "urn:airline:problem:check-in-not-open";

    private static final int HOURS_UNTIL_OPEN_DEPARTURE = 3;
    private static final int HOURS_UNTIL_DISTANT_DEPARTURE = 48;

    /* Sixty seconds because eleven containers share one machine and a new consumer group
     * waits out Kafka's initial rebalance. A limit on patience, not a target. */
    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(60);
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
    private static final String COUNT_PASSES_FOR =
            "SELECT count(*) FROM boarding_passes WHERE booking_id = ?";
    private static final String FIND_UNSENT_MESSAGES =
            "SELECT count(*) FROM outbox WHERE aggregate_id = ? AND published_at IS NULL";
    private static final String COUNT_MESSAGES_FOR =
            "SELECT count(*) FROM outbox WHERE aggregate_id = ?";

    private static final AtomicInteger FLIGHT_SEQUENCE = new AtomicInteger();

    private static RestClient bookingService;
    private static RestClient paymentService;
    private static RestClient checkinService;
    private static JdbcTemplate flightDatabase;
    private static JdbcTemplate bookingDatabase;
    private static JdbcTemplate checkinDatabase;

    @BeforeAll
    static void connect() {
        AirlineStack.start();

        String bearer = logIn();

        bookingService = client(AirlineStack.bookingServiceUrl(), bearer);
        paymentService = client(AirlineStack.paymentServiceUrl(), bearer);
        checkinService = client(AirlineStack.checkinServiceUrl(), bearer);

        flightDatabase = database(AirlineStack.flightDatabaseUrl());
        bookingDatabase = database(AirlineStack.bookingDatabaseUrl());
        checkinDatabase = database(AirlineStack.checkinDatabaseUrl());
    }

    @Nested
    @DisplayName("when the booking is confirmed and the flight is close")
    class Boarding {

        @Test
        @DisplayName("should issue a boarding pass")
        void shouldIssueABoardingPass() {
            String bookingId = aConfirmedBooking(HOURS_UNTIL_OPEN_DEPARTURE);

            Answer answer = checkIn(bookingId);

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CREATED.value());
            Assertions.assertThat(passesFor(bookingId)).isEqualTo(1);
        }

        @Test
        @DisplayName("should print a pass with the flight on it")
        void shouldPrintAPassWithTheFlightOnIt() {
            String bookingId = aConfirmedBooking(HOURS_UNTIL_OPEN_DEPARTURE);

            Answer answer = checkIn(bookingId);

            String flightNumber = JsonPath.read(answer.body(), "$.flightNumber");
            String origin = JsonPath.read(answer.body(), "$.origin");
            int sequence = JsonPath.read(answer.body(), "$.boardingSequence");

            Assertions.assertThat(flightNumber).startsWith("CI");
            Assertions.assertThat(origin).isEqualTo("BOG");
            Assertions.assertThat(sequence).isEqualTo(1);
        }

        @Test
        @DisplayName("should answer with the same pass when asked again")
        void shouldAnswerWithTheSamePassWhenAskedAgain() {
            String bookingId = aConfirmedBooking(HOURS_UNTIL_OPEN_DEPARTURE);

            String first = JsonPath.read(checkIn(bookingId).body(), "$.boardingPassId");
            String again = JsonPath.read(checkIn(bookingId).body(), "$.boardingPassId");

            Assertions.assertThat(again).isEqualTo(first);
            Assertions.assertThat(passesFor(bookingId)).isEqualTo(1);
        }

        @Test
        @DisplayName("should announce the check-in and send what it announced")
        void shouldAnnounceTheCheckInAndSendWhatItAnnounced() {
            String bookingId = aConfirmedBooking(HOURS_UNTIL_OPEN_DEPARTURE);

            checkIn(bookingId);

            Assertions.assertThat(messagesFor(bookingId)).isEqualTo(1);

            await(() -> unsentMessagesFor(bookingId) == 0);
        }
    }

    @Nested
    @DisplayName("when it must not be allowed")
    class Refused {

        @Test
        @DisplayName("should refuse a booking that has not been paid for")
        void shouldRefuseABookingThatHasNotBeenPaidFor() {
            UUID flightId = aFlightDepartingIn(HOURS_UNTIL_OPEN_DEPARTURE);
            String bookingId = aBookingOn(flightId);

            Answer answer = checkIn(bookingId);

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CONFLICT.value());
            Assertions.assertThat(passesFor(bookingId)).isZero();
        }

        @Test
        @DisplayName("should refuse a booking whose payment was declined")
        void shouldRefuseABookingWhosePaymentWasDeclined() {
            UUID flightId = aFlightDepartingIn(HOURS_UNTIL_OPEN_DEPARTURE);
            String bookingId = aBookingOn(flightId);

            pay(bookingId, DECLINING_CARD);
            awaitSettled(bookingId);

            Answer answer = checkIn(bookingId);

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("should refuse a flight that is still two days away")
        void shouldRefuseAFlightThatIsStillTwoDaysAway() {
            String bookingId = aConfirmedBooking(HOURS_UNTIL_DISTANT_DEPARTURE);

            Answer answer = checkIn(bookingId);

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CONFLICT.value());

            String type = JsonPath.read(answer.body(), "$.type");

            Assertions.assertThat(type).isEqualTo(NOT_OPEN_YET);
        }

        @Test
        @DisplayName("should answer not found for a booking nobody has")
        void shouldAnswerNotFoundForABookingNobodyHas() {
            String unknown = UUID.randomUUID().toString();

            Answer answer = checkIn(unknown);

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    private record Answer(int status, String body) {
    }

    private String aConfirmedBooking(final int hoursUntilDeparture) {
        UUID flightId = aFlightDepartingIn(hoursUntilDeparture);
        String bookingId = aBookingOn(flightId);

        pay(bookingId, GOOD_CARD);
        awaitSettled(bookingId);

        Assertions.assertThat(statusOf(bookingId))
                .as("the booking the pass is for")
                .isEqualTo(CONFIRMED);

        return bookingId;
    }

    private UUID aFlightDepartingIn(final int hours) {
        UUID id = UUID.randomUUID();
        int sequence = FLIGHT_SEQUENCE.incrementAndGet();
        String flightNumber = "CI%04d".formatted(sequence);
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusHours(hours);
        OffsetDateTime arrival = departure.plusHours(2);
        BigDecimal fare = new BigDecimal(FARE);

        flightDatabase.update(INSERT_FLIGHT,
                id, flightNumber, departure, arrival, CAPACITY, CAPACITY, fare, COP);

        return id;
    }

    private String aBookingOn(final UUID flightId) {
        String body = """
                {"flightId": "%s", "seats": %d}
                """.formatted(flightId, SEATS_WANTED);

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

    private Answer checkIn(final String bookingId) {
        String body = """
                {"bookingId": "%s"}
                """.formatted(bookingId);

        ResponseEntity<String> response = checkinService.post()
                .uri(BOARDING_PASSES)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        return new Answer(response.getStatusCode().value(), response.getBody());
    }

    private void awaitSettled(final String bookingId) {
        await(() -> !PENDING.equals(statusOf(bookingId)));
    }

    private static void await(final Supplier<Boolean> condition) {
        Instant deadline = Instant.now().plus(SETTLE_TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            if (condition.get()) {
                return;
            }
            sleep();
        }

        throw new AssertionError("Nothing happened within " + SETTLE_TIMEOUT);
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

    private long passesFor(final String bookingId) {
        return checkinDatabase.queryForObject(
                COUNT_PASSES_FOR, Long.class, UUID.fromString(bookingId));
    }

    private long messagesFor(final String bookingId) {
        return checkinDatabase.queryForObject(COUNT_MESSAGES_FOR, Long.class, bookingId);
    }

    private long unsentMessagesFor(final String bookingId) {
        return checkinDatabase.queryForObject(FIND_UNSENT_MESSAGES, Long.class, bookingId);
    }

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String DEMO_EMAIL = "passenger@airline.test";
    private static final String DEMO_PASSWORD = "passenger123";

    private static String logIn() {
        RestClient auth = RestClient.builder().baseUrl(AirlineStack.authServiceUrl()).build();

        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(DEMO_EMAIL, DEMO_PASSWORD);

        String answer = auth.post()
                .uri(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return "Bearer " + JsonPath.read(answer, "$.accessToken");
    }

    private static RestClient client(final String baseUrl, final String bearer) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, bearer)
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
