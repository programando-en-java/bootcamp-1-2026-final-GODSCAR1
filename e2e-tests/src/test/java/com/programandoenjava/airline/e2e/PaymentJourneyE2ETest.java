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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@EnabledIfSystemProperty(named = "airline.e2e", matches = "true")
@DisplayName("Paying for a booking, end to end")
class PaymentJourneyE2ETest {

    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String PAYMENTS = "/api/v1/payments";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String GOOD_CARD = "4242424242424242";
    private static final String DECLINING_CARD = "4000000000000002";

    private static final int SEATS_WANTED = 2;
    private static final int CAPACITY = 120;

    private static final String FARE = "250000.00";
    private static final String COP = "COP";

    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String FAILED = "FAILED";
    private static final String SUCCEEDED = "SUCCEEDED";

    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration BETWEEN_POLLS = Duration.ofMillis(250);

    private static final String INSERT_FLIGHT = """
            INSERT INTO flights (id, flight_number, origin, destination,
                                 departure_time, arrival_time,
                                 total_seats, available_seats,
                                 price_amount, price_currency)
            VALUES (?, ?, 'BOG', 'MDE', ?, ?, ?, ?, ?, ?)
            """;
    private static final String FIND_AVAILABLE_SEATS =
            "SELECT available_seats FROM flights WHERE id = ?";
    private static final String COUNT_SEAT_BLOCKS =
            "SELECT count(*) FROM seat_blocks WHERE flight_id = ?";
    private static final String FIND_BOOKING_STATUS =
            "SELECT status FROM bookings WHERE id = ?";
    private static final String SEATS_WERE_RELEASED =
            "SELECT seats_released_at IS NOT NULL FROM bookings WHERE id = ?";

    private static final AtomicInteger FLIGHT_SEQUENCE = new AtomicInteger();

    private static RestClient bookingService;
    private static RestClient paymentService;
    private static JdbcTemplate flightDatabase;
    private static JdbcTemplate bookingDatabase;

    @BeforeAll
    static void connect() {
        AirlineStack.start();

        bookingService = client(AirlineStack.bookingServiceUrl());
        paymentService = client(AirlineStack.paymentServiceUrl());

        flightDatabase = database(AirlineStack.flightDatabaseUrl());
        bookingDatabase = database(AirlineStack.bookingDatabaseUrl());
    }

    @Nested
    @DisplayName("when the card is charged")
    class Confirmed {

        @Test
        @DisplayName("should confirm the booking")
        void shouldConfirmTheBooking() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            pay(bookingId, GOOD_CARD);

            Assertions.assertThat(settledStatusOf(bookingId)).isEqualTo(CONFIRMED);
        }

        @Test
        @DisplayName("should keep the seats it holds")
        void shouldKeepTheSeatsItHolds() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            pay(bookingId, GOOD_CARD);
            settledStatusOf(bookingId);

            Assertions.assertThat(availableSeats(flightId)).isEqualTo(CAPACITY - SEATS_WANTED);
            Assertions.assertThat(seatBlocksOn(flightId)).isEqualTo(1);
        }

        @Test
        @DisplayName("should charge the fare the booking was made at")
        void shouldChargeTheFareTheBookingWasMadeAt() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            Answer answer = pay(bookingId, GOOD_CARD);

            double total = JsonPath.read(answer.body(), "$.amount");

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CREATED.value());
            Assertions.assertThat(total).isEqualTo(500_000.00);
        }
    }

    @Nested
    @DisplayName("when the card is declined")
    class Failed {

        @Test
        @DisplayName("should answer with a payment that failed rather than an error")
        void shouldAnswerWithAPaymentThatFailedRatherThanAnError() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            Answer answer = pay(bookingId, DECLINING_CARD);

            String status = JsonPath.read(answer.body(), "$.status");

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CREATED.value());
            Assertions.assertThat(status).isNotEqualTo(SUCCEEDED);
        }

        @Test
        @DisplayName("should not confirm the booking")
        void shouldNotConfirmTheBooking() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            pay(bookingId, DECLINING_CARD);

            Assertions.assertThat(settledStatusOf(bookingId)).isEqualTo(FAILED);
        }

        @Test
        @DisplayName("should give the seats back to the flight")
        void shouldGiveTheSeatsBackToTheFlight() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            pay(bookingId, DECLINING_CARD);
            settledStatusOf(bookingId);

            await(() -> availableSeats(flightId) == CAPACITY);

            Assertions.assertThat(availableSeats(flightId)).isEqualTo(CAPACITY);
            Assertions.assertThat(seatBlocksOn(flightId)).isZero();
        }

        @Test
        @DisplayName("should record that the seats went back")
        void shouldRecordThatTheSeatsWentBack() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            pay(bookingId, DECLINING_CARD);
            settledStatusOf(bookingId);

            await(() -> seatsWereReleased(bookingId));

            Assertions.assertThat(seatsWereReleased(bookingId)).isTrue();
        }

        @Test
        @DisplayName("should let the seats be sold to somebody else")
        void shouldLetTheSeatsBeSoldToSomebodyElse() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            pay(bookingId, DECLINING_CARD);
            settledStatusOf(bookingId);
            await(() -> availableSeats(flightId) == CAPACITY);

            String secondBooking = aBookingOn(flightId);

            Assertions.assertThat(secondBooking).isNotEqualTo(bookingId);
            Assertions.assertThat(availableSeats(flightId)).isEqualTo(CAPACITY - SEATS_WANTED);
        }
    }

    @Nested
    @DisplayName("when the booking is not payable")
    class NotPayable {

        @Test
        @DisplayName("should refuse to charge a booking that was already paid for")
        void shouldRefuseToChargeABookingThatWasAlreadyPaidFor() {
            UUID flightId = aBookableFlight();
            String bookingId = aBookingOn(flightId);

            pay(bookingId, GOOD_CARD);
            settledStatusOf(bookingId);

            Answer second = pay(bookingId, GOOD_CARD);

            Assertions.assertThat(second.status()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("should answer nothing for a booking that does not exist")
        void shouldAnswerNothingForABookingThatDoesNotExist() {
            String unknown = UUID.randomUUID().toString();

            Answer answer = pay(unknown, GOOD_CARD);

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }

    private record Answer(int status, String body) {
    }

    private UUID aBookableFlight() {
        UUID id = UUID.randomUUID();
        int sequence = FLIGHT_SEQUENCE.incrementAndGet();
        String flightNumber = "SG%04d".formatted(sequence);
        OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
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

        String created = JsonPath.read(response.getBody(), "$.bookingId");
        String status = JsonPath.read(response.getBody(), "$.status");

        Assertions.assertThat(status).isEqualTo(PENDING);

        return created;
    }

    private Answer pay(final String bookingId, final String cardNumber) {
        String body = """
                {"bookingId": "%s", "cardNumber": "%s"}
                """.formatted(bookingId, cardNumber);

        ResponseEntity<String> response = paymentService.post()
                .uri(PAYMENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        return new Answer(response.getStatusCode().value(), response.getBody());
    }

    private String settledStatusOf(final String bookingId) {
        await(() -> !PENDING.equals(statusOf(bookingId)));

        return statusOf(bookingId);
    }

    private static void await(final Supplier<Boolean> condition) {
        Instant deadline = Instant.now().plus(SETTLE_TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            if (condition.get()) {
                return;
            }
            sleep();
        }

        throw new AssertionError("The saga did not finish within " + SETTLE_TIMEOUT);
    }

    private static void sleep() {
        try {
            Thread.sleep(BETWEEN_POLLS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the saga", interrupted);
        }
    }

    private String statusOf(final String bookingId) {
        return bookingDatabase.queryForObject(
                FIND_BOOKING_STATUS, String.class, UUID.fromString(bookingId));
    }

    private boolean seatsWereReleased(final String bookingId) {
        return bookingDatabase.queryForObject(
                SEATS_WERE_RELEASED, Boolean.class, UUID.fromString(bookingId));
    }

    private int availableSeats(final UUID flightId) {
        return flightDatabase.queryForObject(FIND_AVAILABLE_SEATS, Integer.class, flightId);
    }

    private long seatBlocksOn(final UUID flightId) {
        return flightDatabase.queryForObject(COUNT_SEAT_BLOCKS, Long.class, flightId);
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
