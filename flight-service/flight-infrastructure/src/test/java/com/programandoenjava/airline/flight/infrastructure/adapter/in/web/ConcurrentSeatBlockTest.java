package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.EnableDatabaseTest;
import com.programandoenjava.airline.flight.TestcontainersConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight.FlightPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock.SeatBlockPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.flight.infrastructure.transaction.TransactionSupportConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * The test US-004 rests on: more passengers than seats, all arriving at once.
 *
 * <p>Everything else in this suite runs one request at a time, and a lock that
 * is never contended is indistinguishable from no lock at all. Take
 * {@code @UnitOfWork} off BlockSeatsService and every other test still passes —
 * each port call would open its own transaction, the lock would be released the
 * moment the flight was read, and in sequence nothing would notice. This is the
 * only test that would fail, which is what makes it worth its cost.
 *
 * <p>What is asserted is not "exactly four succeed" but invariants that hold
 * whatever the interleaving: seats sold never exceed the seats there were, and
 * the flight never ends up owing seats it does not have.
 */
@SpringBootTest(classes = {
        FlightController.class,
        GlobalExceptionHandler.class,
        ApplicationConfiguration.class,
        FlightPersistenceConfiguration.class,
        SeatBlockPersistenceConfiguration.class,
        TransactionSupportConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        ValidationAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        DataWebAutoConfiguration.class
})
@AutoConfigureMockMvc
@Sql(scripts = "/db/testdata/R__seed_flights.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Blocking seats under contention")
class ConcurrentSeatBlockTest {

    private static final String FLIGHT = "AV8001";

    /*
     * Eight passengers after nine seats, two each: four can be served, four
     * cannot, and one seat is left over. The leftover matters — with an exact
     * fit the test would pass even if the seats were handed out by luck rather
     * than by the lock.
     */
    private static final int SEATS_AVAILABLE = 9;
    private static final int SEATS_PER_BOOKING = 2;
    private static final int PASSENGERS = 8;

    private static final int GRANTED = HttpStatus.CREATED.value();
    private static final int REFUSED = HttpStatus.CONFLICT.value();

    private static final String SEAT_BLOCKS = "/api/v1/flights/{flightId}/seat-blocks";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String FIND_FLIGHT_ID =
            "SELECT id::text FROM flights WHERE flight_number = ?";
    private static final String FIND_AVAILABLE_SEATS =
            "SELECT available_seats FROM flights WHERE flight_number = ?";
    private static final String COUNT_BLOCKS =
            "SELECT count(*) FROM seat_blocks";
    private static final String SUM_SEATS_HELD =
            "SELECT coalesce(sum(seats), 0) FROM seat_blocks";
    private static final String SET_AVAILABLE_SEATS =
            "UPDATE flights SET available_seats = ? WHERE flight_number = ?";

    private static final int TIMEOUT_SECONDS = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("should never sell more seats than the flight has")
    void shouldNeverSellMoreSeatsThanTheFlightHas() throws Exception {
        leaveOnly(SEATS_AVAILABLE);

        final List<Integer> statuses = blockConcurrently();

        final long granted = countGranted(statuses);
        final long seatsSold = granted * SEATS_PER_BOOKING;

        Assertions.assertThat(seatsSold)
                .as("seats handed out")
                .isLessThanOrEqualTo(SEATS_AVAILABLE);
        Assertions.assertThat(availableSeats())
                .as("seats left on the flight")
                .isEqualTo(SEATS_AVAILABLE - (int) seatsSold);
    }

    /*
     * The losers must be told why. A 500 here would mean the CHECK constraint
     * caught what the lock should have, which is the database saving the
     * application from itself rather than the application being correct.
     */
    @Test
    @DisplayName("should answer the passengers who lost with a conflict, not a failure")
    void shouldAnswerTheLosersWithAConflict() throws Exception {
        leaveOnly(SEATS_AVAILABLE);

        final List<Integer> statuses = blockConcurrently();

        Assertions.assertThat(statuses).containsOnly(GRANTED, REFUSED);
    }

    @Test
    @DisplayName("should record one block for each set of seats it handed out")
    void shouldRecordOneBlockForEachSetOfSeatsItHandedOut() throws Exception {
        leaveOnly(SEATS_AVAILABLE);

        final List<Integer> statuses = blockConcurrently();

        final long granted = countGranted(statuses);

        Assertions.assertThat(blockCount())
                .as("blocks recorded")
                .isEqualTo(granted);
        Assertions.assertThat(seatsHeld())
                .as("seats held across every block")
                .isEqualTo(granted * SEATS_PER_BOOKING);
    }

    /*
     * The latch is what makes this a race. Without it the threads start as the
     * pool gets round to them, which on a fast machine is nearly sequential and
     * the contention never happens.
     */
    private List<Integer> blockConcurrently() throws Exception {
        final CountDownLatch startLine = new CountDownLatch(1);
        final ExecutorService pool = Executors.newFixedThreadPool(PASSENGERS);

        try {
            final List<Callable<Integer>> attempts = IntStream.range(0, PASSENGERS)
                    .<Callable<Integer>>mapToObj(passenger -> () -> {
                        startLine.await();
                        return askForSeats();
                    })
                    .toList();

            final List<Future<Integer>> futures = attempts.stream().map(pool::submit).toList();
            startLine.countDown();

            pool.shutdown();
            final boolean finished = pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assertions.assertThat(finished).as("every attempt finished").isTrue();

            final List<Integer> statuses = new ArrayList<>();
            for (final Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private int askForSeats() throws Exception {
        final String bookingId = UUID.randomUUID().toString();
        final String key = UUID.randomUUID().toString();
        final String body = """
                {"bookingId": "%s", "seats": %d}
                """.formatted(bookingId, SEATS_PER_BOOKING);

        return mockMvc.perform(MockMvcRequestBuilders.post(SEAT_BLOCKS, flightId())
                        .header(IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();
    }

    private static long countGranted(final List<Integer> statuses) {
        return statuses.stream().filter(status -> status == GRANTED).count();
    }

    private String flightId() {
        return jdbcTemplate.queryForObject(FIND_FLIGHT_ID, String.class, FLIGHT);
    }

    private int availableSeats() {
        return jdbcTemplate.queryForObject(FIND_AVAILABLE_SEATS, Integer.class, FLIGHT);
    }

    private long blockCount() {
        return jdbcTemplate.queryForObject(COUNT_BLOCKS, Long.class);
    }

    private long seatsHeld() {
        return jdbcTemplate.queryForObject(SUM_SEATS_HELD, Long.class);
    }

    private void leaveOnly(final int seats) {
        jdbcTemplate.update(SET_AVAILABLE_SEATS, seats, FLIGHT);
    }
}