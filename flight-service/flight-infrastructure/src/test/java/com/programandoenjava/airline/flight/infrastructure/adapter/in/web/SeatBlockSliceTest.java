package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.flight.EnableDatabaseTest;
import com.programandoenjava.airline.flight.TestcontainersConfiguration;
import com.programandoenjava.airline.flight.domain.seatblock.SeatCount;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight.FlightPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock.SeatBlockPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.flight.infrastructure.security.SecurityConfiguration;
import com.programandoenjava.airline.flight.infrastructure.transaction.TransactionSupportConfiguration;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@SpringBootTest(classes = {
        FlightController.class,
        GlobalExceptionHandler.class,
        ApplicationConfiguration.class,
        FlightPersistenceConfiguration.class,
        SeatBlockPersistenceConfiguration.class,
        TransactionSupportConfiguration.class,
        SecurityConfiguration.class
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
@DisplayName("Seat block slice (hand-built, no full context)")
class SeatBlockSliceTest {

    private static final String BOOKABLE = "AV8001";

    private static final String ANOTHER_BOOKABLE = "AV8002";

    private static final String DEPARTED = "AV9000";

    private static final String SEAT_BLOCKS = "/api/v1/flights/{flightId}/seat-blocks";
    private static final String SEAT_BLOCK =
            "/api/v1/flights/{flightId}/seat-blocks/{seatBlockId}";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final int SEATS_WANTED = 2;
    private static final int SEATS_LEFT = 2;
    private static final int ONE_MORE_THAN_LEFT = SEATS_LEFT + 1;
    private static final int NOTHING_LEFT = 0;
    private static final int NO_SEATS = 0;
    private static final int OVER_THE_BOOKING_LIMIT = SeatCount.MAX + 1;

    private static final String ALREADY_HELD = "already holds seats";
    private static final String NOT_ENOUGH_SEATS = "only " + SEATS_LEFT + " available";
    private static final String NOT_BOOKABLE = "no longer open for booking";

    private static final double FARE = 250_000.00;
    private static final double A_CENT = 0.001;
    private static final String CURRENCY = "COP";

    private static final String FIND_FLIGHT_ID =
            "SELECT id::text FROM flights WHERE flight_number = ?";
    private static final String FIND_AVAILABLE_SEATS =
            "SELECT available_seats FROM flights WHERE flight_number = ?";
    private static final String COUNT_BLOCKS =
            "SELECT count(*) FROM seat_blocks";
    private static final String SET_AVAILABLE_SEATS =
            "UPDATE flights SET available_seats = ? WHERE flight_number = ?";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("holding seats")
    class Holding {

        @Test
        @DisplayName("should take the seats off the flight")
        void shouldTakeTheSeatsOffTheFlight() throws Exception {
            int before = availableSeats(BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isCreated());

            Assertions.assertThat(availableSeats(BOOKABLE)).isEqualTo(before - SEATS_WANTED);
        }

        @Test
        @DisplayName("should answer with a block tied to the flight and the booking")
        void shouldAnswerWithABlockTiedToTheFlightAndTheBooking() throws Exception {
            String booking = aBooking();
            String flightId = flightIdOf(BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, booking, SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.seatBlockId").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.flightId").value(flightId))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.bookingId").value(booking))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.seats").value(SEATS_WANTED))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.pricePerSeat")
                            .value(Matchers.closeTo(FARE, A_CENT)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(CURRENCY))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.blockedAt").isNotEmpty());
        }

        @Test
        @DisplayName("should sell the last seats a flight has")
        void shouldSellTheLastSeatsAFlightHas() throws Exception {
            leaveOnly(SEATS_LEFT, BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), SEATS_LEFT, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isCreated());

            Assertions.assertThat(availableSeats(BOOKABLE)).isZero();
        }

        @Test
        @DisplayName("should let two bookings hold seats on the same flight")
        void shouldLetTwoBookingsHoldSeatsOnTheSameFlight() throws Exception {
            int before = availableSeats(BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), SEATS_WANTED, aKey()));
            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), SEATS_WANTED, aKey()));

            Assertions.assertThat(availableSeats(BOOKABLE))
                    .isEqualTo(before - (SEATS_WANTED * 2));
        }
    }

    @Nested
    @DisplayName("asking twice")
    class AskingTwice {

        @Test
        @DisplayName("should answer a repeated request with the block it already made")
        void shouldAnswerARepeatedRequestWithTheBlockItAlreadyMade() throws Exception {
            String booking = aBooking();
            String key = aKey();

            String first = mockMvc.perform(blockRequest(BOOKABLE, booking, SEATS_WANTED, key))
                    .andReturn().getResponse().getContentAsString();
            String second = mockMvc.perform(blockRequest(BOOKABLE, booking, SEATS_WANTED, key))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String firstId = JsonPath.read(first, "$.seatBlockId");
            String secondId = JsonPath.read(second, "$.seatBlockId");

            Assertions.assertThat(secondId).isEqualTo(firstId);
        }

        @Test
        @DisplayName("should not take the seats twice for a repeated request")
        void shouldNotTakeTheSeatsTwiceForARepeatedRequest() throws Exception {
            String booking = aBooking();
            String key = aKey();
            int before = availableSeats(BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, booking, SEATS_WANTED, key));
            mockMvc.perform(blockRequest(BOOKABLE, booking, SEATS_WANTED, key));

            Assertions.assertThat(availableSeats(BOOKABLE)).isEqualTo(before - SEATS_WANTED);
        }

        @Test
        @DisplayName("should refuse a booking that already holds seats under another key")
        void shouldRefuseABookingThatAlreadyHoldsSeatsUnderAnotherKey() throws Exception {
            String booking = aBooking();

            mockMvc.perform(blockRequest(BOOKABLE, booking, SEATS_WANTED, aKey()));

            mockMvc.perform(blockRequest(BOOKABLE, booking, SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString(ALREADY_HELD)));
        }
    }

    @Nested
    @DisplayName("refusing what cannot be sold")
    class Refusing {

        @Test
        @DisplayName("should refuse more seats than are left, and say how many there were")
        void shouldRefuseMoreSeatsThanAreLeft() throws Exception {
            leaveOnly(SEATS_LEFT, BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), ONE_MORE_THAN_LEFT, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString(NOT_ENOUGH_SEATS)));
        }

        @Test
        @DisplayName("should refuse a flight with nothing left to sell")
        void shouldRefuseAFlightWithNothingLeftToSell() throws Exception {
            leaveOnly(NOTHING_LEFT, BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isConflict());
        }

        @Test
        @DisplayName("should refuse a flight that has already departed")
        void shouldRefuseAFlightThatHasAlreadyDeparted() throws Exception {
            mockMvc.perform(blockRequest(DEPARTED, aBooking(), SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString(NOT_BOOKABLE)));
        }

        @Test
        @DisplayName("should leave the seats alone when it refuses")
        void shouldLeaveTheSeatsAloneWhenItRefuses() throws Exception {
            leaveOnly(SEATS_LEFT, BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), ONE_MORE_THAN_LEFT, aKey()));

            Assertions.assertThat(availableSeats(BOOKABLE)).isEqualTo(SEATS_LEFT);
        }

        @Test
        @DisplayName("should record nothing when it refuses")
        void shouldRecordNothingWhenItRefuses() throws Exception {
            leaveOnly(SEATS_LEFT, BOOKABLE);

            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), ONE_MORE_THAN_LEFT, aKey()));

            Assertions.assertThat(blockCount()).isZero();
        }

        @Test
        @DisplayName("should answer nothing for a flight that does not exist")
        void shouldAnswerNothingForAFlightThatDoesNotExist() throws Exception {
            String unknown = UUID.randomUUID().toString();

            mockMvc.perform(post(unknown, aBooking(), SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }
    }

    @Nested
    @DisplayName("releasing seats")
    class Releasing {

        @Test
        @DisplayName("should put the seats back on the flight")
        void shouldPutTheSeatsBackOnTheFlight() throws Exception {
            final int before = availableSeats(BOOKABLE);
            final String seatBlockId = holdSeatsOn(BOOKABLE);

            mockMvc.perform(releaseRequest(BOOKABLE, seatBlockId))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());

            Assertions.assertThat(availableSeats(BOOKABLE)).isEqualTo(before);
        }

        @Test
        @DisplayName("should remove the record of the hold")
        void shouldRemoveTheRecordOfTheHold() throws Exception {
            final String seatBlockId = holdSeatsOn(BOOKABLE);

            mockMvc.perform(releaseRequest(BOOKABLE, seatBlockId));

            Assertions.assertThat(blockCount()).isZero();
        }

        @Test
        @DisplayName("should let the seats be sold again")
        void shouldLetTheSeatsBeSoldAgain() throws Exception {
            leaveOnly(SEATS_LEFT, BOOKABLE);
            final String seatBlockId = holdSeatsOn(BOOKABLE);

            mockMvc.perform(releaseRequest(BOOKABLE, seatBlockId));

            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), SEATS_LEFT, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        }

        @Test
        @DisplayName("should succeed when the hold is already gone")
        void shouldSucceedWhenTheHoldIsAlreadyGone() throws Exception {
            final int before = availableSeats(BOOKABLE);
            final String seatBlockId = holdSeatsOn(BOOKABLE);

            mockMvc.perform(releaseRequest(BOOKABLE, seatBlockId));
            mockMvc.perform(releaseRequest(BOOKABLE, seatBlockId))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());

            Assertions.assertThat(availableSeats(BOOKABLE)).isEqualTo(before);
        }

        @Test
        @DisplayName("should succeed when there was never a hold")
        void shouldSucceedWhenThereWasNeverAHold() throws Exception {
            final String unknown = UUID.randomUUID().toString();

            mockMvc.perform(releaseRequest(BOOKABLE, unknown))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());
        }

        @Test
        @DisplayName("should leave alone a hold that belongs to another flight")
        void shouldLeaveAloneAHoldThatBelongsToAnotherFlight() throws Exception {
            final int before = availableSeats(BOOKABLE);
            final String seatBlockId = holdSeatsOn(BOOKABLE);

            mockMvc.perform(releaseRequest(ANOTHER_BOOKABLE, seatBlockId))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());

            Assertions.assertThat(availableSeats(BOOKABLE)).isEqualTo(before - SEATS_WANTED);
            Assertions.assertThat(blockCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should answer nothing for a flight that does not exist")
        void shouldAnswerNothingForAFlightThatDoesNotExist() throws Exception {
            final String unknownFlight = UUID.randomUUID().toString();
            final String seatBlockId = UUID.randomUUID().toString();

            mockMvc.perform(MockMvcRequestBuilders.delete(SEAT_BLOCK, unknownFlight, seatBlockId)
                            .with(anyCaller()))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }
    }

    @Nested
    @DisplayName("rejecting bad input")
    class RejectingBadInput {

        @Test
        @DisplayName("should reject a request with no idempotency key")
        void shouldRejectARequestWithNoIdempotencyKey() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEAT_BLOCKS, flightIdOf(BOOKABLE))
                            .with(anyCaller())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyFor(aBooking(), SEATS_WANTED)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a party larger than one booking allows")
        void shouldRejectAPartyLargerThanOneBookingAllows() throws Exception {
            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), OVER_THE_BOOKING_LIMIT, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a request for no seats at all")
        void shouldRejectARequestForNoSeatsAtAll() throws Exception {
            mockMvc.perform(blockRequest(BOOKABLE, aBooking(), NO_SEATS, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a request with no booking")
        void shouldRejectARequestWithNoBooking() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(SEAT_BLOCKS, flightIdOf(BOOKABLE))
                            .with(anyCaller())
                            .header(IDEMPOTENCY_KEY, aKey())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"seats": %d}
                                    """.formatted(SEATS_WANTED)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should reject a flight id that is not a uuid")
        void shouldRejectAFlightIdThatIsNotAUuid() throws Exception {
            mockMvc.perform(post("not-a-uuid", aBooking(), SEATS_WANTED, aKey()))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    private String holdSeatsOn(final String flightNumber) throws Exception {
        String body = mockMvc.perform(blockRequest(flightNumber, aBooking(), SEATS_WANTED, aKey()))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.seatBlockId");
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor anyCaller() {
        return SecurityMockMvcRequestPostProcessors.jwt();
    }

    private MockHttpServletRequestBuilder releaseRequest(final String flightNumber,
                                                         final String seatBlockId) {
        return MockMvcRequestBuilders.delete(SEAT_BLOCK, flightIdOf(flightNumber), seatBlockId)
                .with(anyCaller());
    }

    private MockHttpServletRequestBuilder blockRequest(final String flightNumber,
                                                       final String bookingId,
                                                       final int seats,
                                                       final String key) {
        return post(flightIdOf(flightNumber), bookingId, seats, key);
    }

    private MockHttpServletRequestBuilder post(final String flightId,
                                               final String bookingId,
                                               final int seats,
                                               final String key) {
        return MockMvcRequestBuilders.post(SEAT_BLOCKS, flightId)
                .with(anyCaller())
                .header(IDEMPOTENCY_KEY, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyFor(bookingId, seats));
    }

    private static String bodyFor(final String bookingId, final int seats) {
        return """
                {"bookingId": "%s", "seats": %d}
                """.formatted(bookingId, seats);
    }

    private String flightIdOf(final String flightNumber) {
        return jdbcTemplate.queryForObject(FIND_FLIGHT_ID, String.class, flightNumber);
    }

    private int availableSeats(final String flightNumber) {
        return jdbcTemplate.queryForObject(FIND_AVAILABLE_SEATS, Integer.class, flightNumber);
    }

    private long blockCount() {
        return jdbcTemplate.queryForObject(COUNT_BLOCKS, Long.class);
    }

    private void leaveOnly(final int seats, final String flightNumber) {
        jdbcTemplate.update(SET_AVAILABLE_SEATS, seats, flightNumber);
    }

    private static String aBooking() {
        return UUID.randomUUID().toString();
    }

    private static String aKey() {
        return UUID.randomUUID().toString();
    }
}