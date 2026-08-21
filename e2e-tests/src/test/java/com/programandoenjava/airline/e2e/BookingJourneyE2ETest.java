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
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@EnabledIfSystemProperty(named = "airline.e2e", matches = "true")
@DisplayName("Booking a flight, end to end")
class BookingJourneyE2ETest {

    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final int SEATS_WANTED = 2;
    private static final int PLENTY_OF_SEATS = 120;
    private static final int NOT_ENOUGH_SEATS = 1;
    private static final int NO_SEATS = 0;

    private static final String FARE = "250000.00";
    private static final double TOTAL_FOR_TWO = 500_000.00;
    private static final String COP = "COP";

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
    private static final String FIND_SEAT_BLOCK_KEY =
            "SELECT idempotency_key FROM seat_blocks WHERE flight_id = ?";

    private static final AtomicInteger FLIGHT_SEQUENCE = new AtomicInteger();

    private static RestClient bookingService;
    private static JdbcTemplate flightDatabase;

    @BeforeAll
    static void connect() {
        AirlineStack.start();

        bookingService = RestClient.builder()
                .baseUrl(AirlineStack.bookingServiceUrl())
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                })
                .build();

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                AirlineStack.flightDatabaseUrl(),
                AirlineStack.databaseUser(),
                AirlineStack.databasePassword());
        flightDatabase = new JdbcTemplate(dataSource);
    }

    @Nested
    @DisplayName("when the flight has room")
    class Bookable {

        @Test
        @DisplayName("should make the booking and take the seats off the flight")
        void shouldMakeTheBookingAndTakeTheSeatsOffTheFlight() {
            UUID flightId = aFlightWith(PLENTY_OF_SEATS, departingTomorrow());

            Answer answer = book(flightId, SEATS_WANTED, aKey());

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CREATED.value());
            Assertions.assertThat(availableSeats(flightId))
                    .isEqualTo(PLENTY_OF_SEATS - SEATS_WANTED);
        }

        @Test
        @DisplayName("should charge the fare flight-service quoted")
        void shouldChargeTheFareFlightServiceQuoted() {
            UUID flightId = aFlightWith(PLENTY_OF_SEATS, departingTomorrow());

            Answer answer = book(flightId, SEATS_WANTED, aKey());

            double total = JsonPath.read(answer.body(), "$.total");

            Assertions.assertThat(total).isEqualTo(TOTAL_FOR_TWO);
        }

        @Test
        @DisplayName("should pass its idempotency key through to flight-service")
        void shouldPassItsIdempotencyKeyThroughToFlightService() {
            UUID flightId = aFlightWith(PLENTY_OF_SEATS, departingTomorrow());
            String key = aKey();

            book(flightId, SEATS_WANTED, key);

            Assertions.assertThat(seatBlockKeyFor(flightId)).isEqualTo(key);
        }
    }

    @Nested
    @DisplayName("when the same request arrives twice")
    class Repeated {

        @Test
        @DisplayName("should answer with the booking it already made")
        void shouldAnswerWithTheBookingItAlreadyMade() {
            UUID flightId = aFlightWith(PLENTY_OF_SEATS, departingTomorrow());
            String key = aKey();

            Answer first = book(flightId, SEATS_WANTED, key);
            Answer second = book(flightId, SEATS_WANTED, key);

            String firstId = JsonPath.read(first.body(), "$.bookingId");
            String secondId = JsonPath.read(second.body(), "$.bookingId");

            Assertions.assertThat(secondId).isEqualTo(firstId);
        }

        @Test
        @DisplayName("should take the seats once")
        void shouldTakeTheSeatsOnce() {
            UUID flightId = aFlightWith(PLENTY_OF_SEATS, departingTomorrow());
            String key = aKey();

            book(flightId, SEATS_WANTED, key);
            book(flightId, SEATS_WANTED, key);

            Assertions.assertThat(availableSeats(flightId))
                    .isEqualTo(PLENTY_OF_SEATS - SEATS_WANTED);
        }
    }

    @Nested
    @DisplayName("when flight-service refuses")
    class Refused {

        @Test
        @DisplayName("should answer with a conflict when the seats are gone")
        void shouldAnswerWithAConflictWhenTheSeatsAreGone() {
            UUID flightId = aFlightWith(NOT_ENOUGH_SEATS, departingTomorrow());

            Answer answer = book(flightId, SEATS_WANTED, aKey());

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CONFLICT.value());

            String title = JsonPath.read(answer.body(), "$.title");

            Assertions.assertThat(title).isEqualTo("Not enough seats");
        }

        @Test
        @DisplayName("should answer with a conflict when the flight is sold out")
        void shouldAnswerWithAConflictWhenTheFlightIsSoldOut() {
            UUID flightId = aFlightWith(NO_SEATS, departingTomorrow());

            Answer answer = book(flightId, SEATS_WANTED, aKey());

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("should answer that a departed flight cannot be booked")
        void shouldAnswerThatADepartedFlightCannotBeBooked() {
            UUID flightId = aFlightWith(PLENTY_OF_SEATS, departedYesterday());

            Answer answer = book(flightId, SEATS_WANTED, aKey());

            Assertions.assertThat(answer.status())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        }

        @Test
        @DisplayName("should answer that a flight nobody has heard of does not exist")
        void shouldAnswerThatAFlightNobodyHasHeardOfDoesNotExist() {
            UUID unknown = UUID.randomUUID();

            Answer answer = book(unknown, SEATS_WANTED, aKey());

            Assertions.assertThat(answer.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @DisplayName("should record no booking when it refuses")
        void shouldRecordNoBookingWhenItRefuses() {
            UUID flightId = aFlightWith(NOT_ENOUGH_SEATS, departingTomorrow());

            book(flightId, SEATS_WANTED, aKey());

            Assertions.assertThat(seatBlocksFor(flightId)).isZero();
        }
    }

    private record Answer(int status, String body) {
    }

    private Answer book(final UUID flightId, final int seats, final String key) {
        String body = bodyFor(flightId, seats);

        ResponseEntity<String> response = bookingService.post()
                .uri(BOOKINGS)
                .header(IDEMPOTENCY_KEY, key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        int status = response.getStatusCode().value();

        return new Answer(status, response.getBody());
    }

    private static String bodyFor(final UUID flightId, final int seats) {
        UUID passengerId = UUID.randomUUID();

        return """
                {"passengerId": "%s", "flightId": "%s", "seats": %d}
                """.formatted(passengerId, flightId, seats);
    }

    private UUID aFlightWith(final int availableSeats, final OffsetDateTime departure) {
        UUID id = UUID.randomUUID();
        int sequence = FLIGHT_SEQUENCE.incrementAndGet();
        String flightNumber = "E2%04d".formatted(sequence);
        OffsetDateTime arrival = departure.plusHours(1);
        int capacity = Math.max(availableSeats, 1);
        BigDecimal fare = new BigDecimal(FARE);

        flightDatabase.update(INSERT_FLIGHT,
                id, flightNumber, departure, arrival,
                capacity, availableSeats, fare, COP);

        return id;
    }

    private int availableSeats(final UUID flightId) {
        return flightDatabase.queryForObject(FIND_AVAILABLE_SEATS, Integer.class, flightId);
    }

    private long seatBlocksFor(final UUID flightId) {
        return flightDatabase.queryForObject(COUNT_SEAT_BLOCKS, Long.class, flightId);
    }

    private String seatBlockKeyFor(final UUID flightId) {
        return flightDatabase.queryForObject(FIND_SEAT_BLOCK_KEY, String.class, flightId);
    }

    private static OffsetDateTime departingTomorrow() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
    }

    private static OffsetDateTime departedYesterday() {
        return OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
    }

    private static String aKey() {
        return UUID.randomUUID().toString();
    }
}
