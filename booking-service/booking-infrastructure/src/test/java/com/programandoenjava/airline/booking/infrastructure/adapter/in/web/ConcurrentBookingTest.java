package com.programandoenjava.airline.booking.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.booking.EnableDatabaseTest;
import com.programandoenjava.airline.booking.TestcontainersConfiguration;
import com.programandoenjava.airline.booking.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.SeatsHeld;
import com.programandoenjava.airline.booking.application.port.out.processedevents.ProcessedEventsPort;
import com.programandoenjava.airline.booking.application.port.out.releaseseats.ReleaseSeatsPort;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import com.programandoenjava.airline.booking.domain.shared.Money;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking.BookingPersistenceConfiguration;
import com.programandoenjava.airline.booking.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.booking.infrastructure.transaction.TransactionSupportConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@SpringBootTest(classes = {
        BookingController.class,
        GlobalExceptionHandler.class,
        ApplicationConfiguration.class,
        BookingPersistenceConfiguration.class,
        TransactionSupportConfiguration.class
})
@EnableDatabaseTest
@Import(TestcontainersConfiguration.class)
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        ValidationAutoConfiguration.class,
        JacksonAutoConfiguration.class
})
@AutoConfigureMockMvc
@Sql(scripts = "/db/testdata/R__reset_bookings.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Booking the same request twice at once")
/* The only test that fails if the serialisable isolation comes off BookingRecorder:
 * one request at a time never notices its isolation level (ADR-019). */
class ConcurrentBookingTest {

    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final int CLICKS = 8;
    private static final int SEATS_WANTED = 2;

    private static final int CREATED = HttpStatus.CREATED.value();

    private static final String COP = "COP";
    private static final String FARE = "250000.00";

    private static final String COUNT_BOOKINGS = "SELECT count(*) FROM bookings";

    private static final int TIMEOUT_SECONDS = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private HoldSeatsPort holdSeatsPort;

    @MockitoBean
    private ProcessedEventsPort processedEventsPort;

    @MockitoBean
    private ReleaseSeatsPort releaseSeatsPort;

    @MockitoBean
    private DomainEventPublisher domainEventPublisher;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("should make one booking, however many of them arrive together")
    void shouldMakeOneBookingHoweverManyArriveTogether() throws Exception {
        givenSeatsAreHeld();

        clickManyTimes();

        Assertions.assertThat(bookingCount())
                .as("bookings written")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("should answer every one of them, and none with a failure")
    void shouldAnswerEveryOneOfThemAndNoneWithAFailure() throws Exception {
        givenSeatsAreHeld();

        List<Integer> statuses = clickManyTimes().stream().map(Answer::status).toList();

        Assertions.assertThat(statuses).containsOnly(CREATED);
    }

    @Test
    @DisplayName("should tell all of them about the same booking")
    void shouldTellAllOfThemAboutTheSameBooking() throws Exception {
        givenSeatsAreHeld();

        List<String> bookingIds = clickManyTimes().stream()
                .map(Answer::bookingId)
                .distinct()
                .toList();

        Assertions.assertThat(bookingIds)
                .as("distinct bookings handed back")
                .hasSize(1);
    }

    private record Answer(int status, String bookingId) {
    }

    /* The latch is what makes this a race: without it the pool runs them nearly in order. */
    private List<Answer> clickManyTimes() throws Exception {
        UUID passenger = UUID.randomUUID();
        UUID flight = UUID.randomUUID();
        String key = UUID.randomUUID().toString();

        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CLICKS);

        try {
            List<Callable<Answer>> clicks = IntStream.range(0, CLICKS)
                    .<Callable<Answer>>mapToObj(click -> () -> {
                        startLine.await();
                        return book(passenger, flight, key);
                    })
                    .toList();

            List<Future<Answer>> futures = clicks.stream().map(pool::submit).toList();
            startLine.countDown();

            pool.shutdown();
            boolean finished = pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assertions.assertThat(finished).as("every click finished").isTrue();

            List<Answer> answers = new ArrayList<>();
            for (Future<Answer> future : futures) {
                answers.add(future.get());
            }
            return answers;
        } finally {
            pool.shutdownNow();
        }
    }

    private Answer book(final UUID passengerId, final UUID flightId, final String key)
            throws Exception {

        String body = """
                {"passengerId": "%s", "flightId": "%s", "seats": %d}
                """.formatted(passengerId, flightId, SEATS_WANTED);

        var response = mockMvc.perform(MockMvcRequestBuilders.post(BOOKINGS)
                        .header(IDEMPOTENCY_KEY, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse();

        int status = response.getStatus();
        String payload = response.getContentAsString();

        return new Answer(status, bookingIdIn(status, payload));
    }

    private static String bookingIdIn(final int status, final String payload) {
        boolean created = status == CREATED;

        if (!created) {
            return payload;
        }

        return JsonPath.read(payload, "$.bookingId");
    }

    private void givenSeatsAreHeld() {
        SeatsHeld held = new SeatsHeld(
                new SeatBlockId(UUID.randomUUID()),
                new Money(new BigDecimal(FARE), Currency.getInstance(COP)));

        BDDMockito.given(holdSeatsPort.hold(BDDMockito.any())).willReturn(held);
    }

    private long bookingCount() {
        return jdbcTemplate.queryForObject(COUNT_BOOKINGS, Long.class);
    }
}
