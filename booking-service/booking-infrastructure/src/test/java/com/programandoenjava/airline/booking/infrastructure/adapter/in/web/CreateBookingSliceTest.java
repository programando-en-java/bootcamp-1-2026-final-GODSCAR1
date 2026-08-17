package com.programandoenjava.airline.booking.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.booking.EnableDatabaseTest;
import com.programandoenjava.airline.booking.TestcontainersConfiguration;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsCommand;
import com.programandoenjava.airline.booking.application.port.out.holdseats.HoldSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.holdseats.SeatsHeld;
import com.programandoenjava.airline.booking.application.port.out.holdseats.exception.SeatsUnavailableException;
import com.programandoenjava.airline.booking.domain.booking.SeatBlockId;
import com.programandoenjava.airline.booking.domain.shared.Money;
import com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking.BookingPersistenceConfiguration;
import com.programandoenjava.airline.booking.infrastructure.config.ApplicationConfiguration;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;

@SpringBootTest(classes = {
        BookingController.class,
        GlobalExceptionHandler.class,
        ApplicationConfiguration.class,
        BookingPersistenceConfiguration.class
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
@DisplayName("Create booking slice (hand-built, no full context)")
class CreateBookingSliceTest {

    private static final String BOOKINGS = "/api/v1/bookings";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String COP = "COP";
    private static final String FARE = "250000.00";
    private static final double FARE_AS_NUMBER = 250_000.00;
    private static final double TOTAL_FOR_TWO = 500_000.00;
    private static final double A_CENT = 0.001;

    private static final int SEATS_WANTED = 2;
    private static final int NO_SEATS = 0;
    private static final int OVER_THE_BOOKING_LIMIT = 10;

    private static final String PENDING = "PENDING";
    private static final String CREATED_AT = "2026-03-10T12:00:00Z";

    private static final String COUNT_BOOKINGS = "SELECT count(*) FROM bookings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * flight-service is mocked on purpose. What this slice checks is what
     * booking-service decides on its own: that one key yields one booking, and
     * that a retry never reaches the port at all. Whether flight-service answers
     * the way the adapter expects is a question for the end-to-end test, which
     * is also the only place the Feign error decoder runs.
     */
    @MockitoBean
    private HoldSeatsPort holdSeatsPort;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse(CREATED_AT), ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("making a booking")
    class Making {

        @Test
        @DisplayName("should answer with the booking it made")
        void shouldAnswerWithTheBookingItMade() throws Exception {
            UUID passenger = aPassenger();
            UUID flight = aFlight();
            givenSeatsAreHeld();

            mockMvc.perform(booking(passenger, flight, SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.bookingId").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.passengerId")
                            .value(passenger.toString()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.flightId")
                            .value(flight.toString()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.seats").value(SEATS_WANTED))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(PENDING));
        }

        @Test
        @DisplayName("should charge the fare once per seat")
        void shouldChargeTheFareOncePerSeat() throws Exception {
            givenSeatsAreHeld();

            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.pricePerSeat")
                            .value(Matchers.closeTo(FARE_AS_NUMBER, A_CENT)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.total")
                            .value(Matchers.closeTo(TOTAL_FOR_TWO, A_CENT)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(COP));
        }

        @Test
        @DisplayName("should record when it was made, by the application's clock")
        void shouldRecordWhenItWasMade() throws Exception {
            givenSeatsAreHeld();

            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.createdAt").value(CREATED_AT));
        }

        @Test
        @DisplayName("should ask flight-service for the seats the passenger wanted")
        void shouldAskFlightServiceForTheSeatsThePassengerWanted() throws Exception {
            UUID flight = aFlight();
            String key = aKey();
            givenSeatsAreHeld();

            mockMvc.perform(booking(aPassenger(), flight, SEATS_WANTED, key));

            HoldSeatsCommand asked = whatWasAsked();

            Assertions.assertThat(asked.flightId().value()).isEqualTo(flight);
            Assertions.assertThat(asked.seats().value()).isEqualTo(SEATS_WANTED);
            Assertions.assertThat(asked.idempotencyKey().value()).isEqualTo(key);
        }

        /*
         * The key goes out as a field of its own rather than folded into the
         * booking id. flight-service uses the two for different questions, and
         * passing ours through is what makes a retry one request in their eyes
         * as well as ours (ADR-011).
         */
        @Test
        @DisplayName("should send the booking id and the key as separate things")
        void shouldSendTheBookingIdAndTheKeyAsSeparateThings() throws Exception {
            String key = aKey();
            givenSeatsAreHeld();

            String body = mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, key))
                    .andReturn().getResponse().getContentAsString();

            String bookingId = JsonPath.read(body, "$.bookingId");
            HoldSeatsCommand asked = whatWasAsked();

            Assertions.assertThat(asked.bookingId().value().toString()).isEqualTo(bookingId);
            Assertions.assertThat(bookingId).isNotEqualTo(key);
        }
    }

    @Nested
    @DisplayName("asking twice")
    class AskingTwice {

        @Test
        @DisplayName("should answer a repeated request with the booking it already made")
        void shouldAnswerARepeatedRequestWithTheBookingItAlreadyMade() throws Exception {
            UUID passenger = aPassenger();
            UUID flight = aFlight();
            String key = aKey();
            givenSeatsAreHeld();

            String first = mockMvc.perform(booking(passenger, flight, SEATS_WANTED, key))
                    .andReturn().getResponse().getContentAsString();
            String second = mockMvc.perform(booking(passenger, flight, SEATS_WANTED, key))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String firstId = JsonPath.read(first, "$.bookingId");
            String secondId = JsonPath.read(second, "$.bookingId");

            Assertions.assertThat(secondId).isEqualTo(firstId);
        }

        @Test
        @DisplayName("should make only one booking for a repeated request")
        void shouldMakeOnlyOneBookingForARepeatedRequest() throws Exception {
            String key = aKey();
            givenSeatsAreHeld();

            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, key));
            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, key));

            Assertions.assertThat(bookingCount()).isEqualTo(1);
        }

        /*
         * The one thing an end-to-end test cannot say without instrumenting
         * flight-service: the retry never reached it.
         */
        @Test
        @DisplayName("should not ask flight-service twice for a repeated request")
        void shouldNotAskFlightServiceTwiceForARepeatedRequest() throws Exception {
            String key = aKey();
            givenSeatsAreHeld();

            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, key));
            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, key));

            Mockito.verify(holdSeatsPort, Mockito.times(1))
                    .hold(BDDMockito.any(HoldSeatsCommand.class));
        }

        @Test
        @DisplayName("should make a second booking for a second key")
        void shouldMakeASecondBookingForASecondKey() throws Exception {
            givenSeatsAreHeld();

            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, aKey()));
            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, aKey()));

            Assertions.assertThat(bookingCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("when the seats cannot be held")
    class SeatsRefused {

        @Test
        @DisplayName("should answer with a conflict")
        void shouldAnswerWithAConflict() throws Exception {
            givenSeatsAreRefused();

            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title")
                            .value("Not enough seats"));
        }

        @Test
        @DisplayName("should record no booking")
        void shouldRecordNoBooking() throws Exception {
            givenSeatsAreRefused();

            mockMvc.perform(booking(aPassenger(), aFlight(), SEATS_WANTED, aKey()));

            Assertions.assertThat(bookingCount()).isZero();
        }
    }

    @Nested
    @DisplayName("rejecting bad input")
    class RejectingBadInput {

        @Test
        @DisplayName("should reject a request with no idempotency key")
        void shouldRejectARequestWithNoIdempotencyKey() throws Exception {
            String body = bodyFor(aPassenger(), aFlight(), SEATS_WANTED);

            mockMvc.perform(MockMvcRequestBuilders.post(BOOKINGS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a party larger than one booking allows")
        void shouldRejectAPartyLargerThanOneBookingAllows() throws Exception {
            mockMvc.perform(booking(aPassenger(), aFlight(), OVER_THE_BOOKING_LIMIT, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a request for no seats at all")
        void shouldRejectARequestForNoSeatsAtAll() throws Exception {
            mockMvc.perform(booking(aPassenger(), aFlight(), NO_SEATS, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a request with no passenger")
        void shouldRejectARequestWithNoPassenger() throws Exception {
            String body = """
                    {"flightId": "%s", "seats": %d}
                    """.formatted(aFlight(), SEATS_WANTED);

            mockMvc.perform(MockMvcRequestBuilders.post(BOOKINGS)
                            .header(IDEMPOTENCY_KEY, aKey())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should not ask flight-service for a request it refuses")
        void shouldNotAskFlightServiceForARequestItRefuses() throws Exception {
            mockMvc.perform(booking(aPassenger(), aFlight(), NO_SEATS, aKey()));

            Mockito.verifyNoInteractions(holdSeatsPort);
        }
    }

    /*
     * A different hold per call, because that is what flight-service does. A
     * fixed one made the second booking collide with uq_bookings_seat_block,
     * which is the index that stops two bookings claiming the same seats.
     */
    private void givenSeatsAreHeld() {
        Currency currency = Currency.getInstance(COP);
        BigDecimal amount = new BigDecimal(FARE);
        Money fare = new Money(amount, currency);

        BDDMockito.given(holdSeatsPort.hold(BDDMockito.any()))
                .willAnswer(invocation -> {
                    SeatBlockId seatBlockId = new SeatBlockId(UUID.randomUUID());
                    return new SeatsHeld(seatBlockId, fare);
                });
    }

    private void givenSeatsAreRefused() {
        SeatsUnavailableException refused =
                new SeatsUnavailableException("The flight has no seats left to hold");

        BDDMockito.given(holdSeatsPort.hold(BDDMockito.any())).willThrow(refused);
    }

    private HoldSeatsCommand whatWasAsked() {
        ArgumentCaptor<HoldSeatsCommand> captor = ArgumentCaptor.forClass(HoldSeatsCommand.class);
        Mockito.verify(holdSeatsPort).hold(captor.capture());

        return captor.getValue();
    }

    private MockHttpServletRequestBuilder booking(final UUID passengerId,
                                                  final UUID flightId,
                                                  final int seats,
                                                  final String key) {
        String body = bodyFor(passengerId, flightId, seats);

        return MockMvcRequestBuilders.post(BOOKINGS)
                .header(IDEMPOTENCY_KEY, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static String bodyFor(final UUID passengerId, final UUID flightId, final int seats) {
        return """
                {"passengerId": "%s", "flightId": "%s", "seats": %d}
                """.formatted(passengerId, flightId, seats);
    }

    private long bookingCount() {
        return jdbcTemplate.queryForObject(COUNT_BOOKINGS, Long.class);
    }

    private static UUID aPassenger() {
        return UUID.randomUUID();
    }

    private static UUID aFlight() {
        return UUID.randomUUID();
    }

    private static String aKey() {
        return UUID.randomUUID().toString();
    }
}
