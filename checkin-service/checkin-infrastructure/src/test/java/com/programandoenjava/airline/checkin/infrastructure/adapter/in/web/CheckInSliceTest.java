package com.programandoenjava.airline.checkin.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.checkin.EnableDatabaseTest;
import com.programandoenjava.airline.checkin.TestcontainersConfiguration;
import com.programandoenjava.airline.checkin.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.BookingToCheckIn;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.ReadBookingPort;
import com.programandoenjava.airline.checkin.application.port.out.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.checkin.application.port.out.readflight.ReadFlightPort;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
import com.programandoenjava.airline.checkin.domain.boardingpass.PassengerId;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass.BoardingPassPersistenceConfiguration;
import com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence.BoardingSequenceConfiguration;
import com.programandoenjava.airline.checkin.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.checkin.infrastructure.transaction.TransactionSupportConfiguration;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Checking in, against a real database. US-007 and US-008 both live here.
 *
 * <p>booking-service and flight-service are mocked at the port: what they answer
 * is their business, and that the two of them and this one agree is what the
 * end-to-end test is for. What cannot be mocked and is the reason for a real
 * database: one pass per booking, and the boarding order.
 *
 * <p>The clock is pinned because every rule in here is about time. Departures
 * are expressed as distances from that instant rather than as literal dates, so
 * a test says "inside the window" instead of leaving the reader to subtract.
 */
@SpringBootTest(classes = {
        CheckInController.class,
        GlobalExceptionHandler.class,
        ApplicationConfiguration.class,
        BoardingPassPersistenceConfiguration.class,
        BoardingSequenceConfiguration.class,
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
@Sql(scripts = "/db/testdata/R__reset_boarding_passes.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Check-in slice (hand-built, no full context)")
class CheckInSliceTest {

    private static final String BOARDING_PASSES = "/api/v1/boarding-passes";

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");

    /** Inside the window: twenty hours ahead, so open and not yet closing. */
    private static final Instant SOON = NOW.plus(Duration.ofHours(20));

    private static final Instant TOO_FAR_AHEAD = NOW.plus(Duration.ofHours(48));
    private static final Instant ABOUT_TO_LEAVE = NOW.plus(Duration.ofMinutes(30));
    private static final Instant ALREADY_GONE = NOW.minus(Duration.ofHours(1));

    private static final String FLIGHT_NUMBER = "AV8001";
    private static final String ORIGIN = "BOG";
    private static final String DESTINATION = "MDE";

    private static final String CONFIRMED = "CONFIRMED";
    private static final String PENDING = "PENDING";
    private static final String FAILED = "FAILED";

    private static final String COUNT_PASSES = "SELECT count(*) FROM boarding_passes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReadBookingPort readBookingPort;

    @MockitoBean
    private ReadFlightPort readFlightPort;

    /*
     * Announcing is its own concern and has its own test. What matters here is
     * that nothing in this file changes when it arrives.
     */
    @MockitoBean
    private DomainEventPublisher domainEventPublisher;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("checking in")
    class CheckingIn {

        @Test
        @DisplayName("should issue a pass for a confirmed booking")
        void shouldIssueAPassForAConfirmedBooking() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.boardingPassId").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.bookingId").value(booking.toString()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.issuedAt").value(NOW.toString()));
        }

        /*
         * The fields copied off the flight. This is the whole reason
         * flight-service is read at all, so it is asserted rather than assumed.
         */
        @Test
        @DisplayName("should print the flight it was read from")
        void shouldPrintTheFlightItWasReadFrom() throws Exception {
            UUID booking = aBooking();
            UUID flight = aFlight();
            givenBookingIsConfirmed(booking, flight, SOON);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.flightId").value(flight.toString()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.flightNumber").value(FLIGHT_NUMBER))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.origin").value(ORIGIN))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.destination").value(DESTINATION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.departureTime").value(SOON.toString()));
        }

        @Test
        @DisplayName("should give the first passenger the first place in the queue")
        void shouldGiveTheFirstPassengerTheFirstPlaceInTheQueue() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.boardingSequence").value(1));
        }

        @Test
        @DisplayName("should give the next passenger on that flight the next place")
        void shouldGiveTheNextPassengerOnThatFlightTheNextPlace() throws Exception {
            UUID flight = aFlight();
            UUID first = aBooking();
            UUID second = aBooking();
            givenBookingIsConfirmed(first, flight, SOON);
            givenBookingIsConfirmed(second, flight, SOON);

            mockMvc.perform(checkIn(first));

            mockMvc.perform(checkIn(second))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.boardingSequence").value(2));
        }

        /*
         * The order is per flight, not global. Without this, the second flight
         * of the day would start boarding at whatever number the first one
         * happened to reach.
         */
        @Test
        @DisplayName("should start another flight at the first place again")
        void shouldStartAnotherFlightAtTheFirstPlaceAgain() throws Exception {
            UUID onOneFlight = aBooking();
            UUID onAnother = aBooking();
            givenBookingIsConfirmed(onOneFlight, aFlight(), SOON);
            givenBookingIsConfirmed(onAnother, aFlight(), SOON);

            mockMvc.perform(checkIn(onOneFlight));

            mockMvc.perform(checkIn(onAnother))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.boardingSequence").value(1));
        }

        @Test
        @DisplayName("should keep the pass, not just answer with it")
        void shouldKeepThePass() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking));

            Assertions.assertThat(passCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("checking in again")
    class Repeating {

        @Test
        @DisplayName("should answer with the pass it already issued")
        void shouldAnswerWithThePassItAlreadyIssued() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), SOON);

            String first = passIdFrom(mockMvc.perform(checkIn(booking))
                    .andReturn().getResponse().getContentAsString());

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.status().isCreated())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.boardingPassId").value(first));
        }

        @Test
        @DisplayName("should not print a second pass")
        void shouldNotPrintASecondPass() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking));
            mockMvc.perform(checkIn(booking));

            Assertions.assertThat(passCount()).isEqualTo(1);
        }

        /*
         * A passenger refreshing the page must not push everyone else back a
         * place. The number is taken only when a pass is actually issued.
         */
        @Test
        @DisplayName("should not take another place in the boarding order")
        void shouldNotTakeAnotherPlaceInTheBoardingOrder() throws Exception {
            UUID flight = aFlight();
            UUID first = aBooking();
            UUID second = aBooking();
            givenBookingIsConfirmed(first, flight, SOON);
            givenBookingIsConfirmed(second, flight, SOON);

            mockMvc.perform(checkIn(first));
            mockMvc.perform(checkIn(first));

            mockMvc.perform(checkIn(second))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.boardingSequence").value(2));
        }

        @Test
        @DisplayName("should answer without asking booking-service again")
        void shouldAnswerWithoutAskingBookingServiceAgain() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking));
            mockMvc.perform(checkIn(booking));

            Mockito.verify(readBookingPort, Mockito.times(1)).byId(new BookingId(booking));
        }
    }

    @Nested
    @DisplayName("when the booking is not one to board with")
    class NotBoardable {

        @Test
        @DisplayName("should refuse a booking that has not been paid for")
        void shouldRefuseABookingThatHasNotBeenPaidFor() throws Exception {
            UUID booking = aBooking();
            givenBookingIs(PENDING, booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title")
                            .value("Booking is not confirmed"));
        }

        @Test
        @DisplayName("should refuse a booking whose payment failed")
        void shouldRefuseABookingWhosePaymentFailed() throws Exception {
            UUID booking = aBooking();
            givenBookingIs(FAILED, booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.status().isConflict());
        }

        /*
         * US-008 asks that the passenger be told what is wrong. Naming the
         * status is the difference between "no" and "pay for it first".
         */
        @Test
        @DisplayName("should say what state the booking is in")
        void shouldSayWhatStateTheBookingIsIn() throws Exception {
            UUID booking = aBooking();
            givenBookingIs(PENDING, booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString(PENDING)));
        }

        @Test
        @DisplayName("should answer not found when there is no such booking")
        void shouldAnswerNotFoundWhenThereIsNoSuchBooking() throws Exception {
            UUID booking = aBooking();
            givenNoSuchBooking(booking);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("should issue nothing when it refuses")
        void shouldIssueNothingWhenItRefuses() throws Exception {
            UUID booking = aBooking();
            givenBookingIs(PENDING, booking, aFlight(), SOON);

            mockMvc.perform(checkIn(booking));

            Assertions.assertThat(passCount()).isZero();
        }
    }

    @Nested
    @DisplayName("when the window is shut")
    class OutsideTheWindow {

        @Test
        @DisplayName("should refuse two days before departure")
        void shouldRefuseTwoDaysBeforeDeparture() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), TOO_FAR_AHEAD);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title")
                            .value("Check-in has not opened"));
        }

        @Test
        @DisplayName("should tell the passenger when it will open")
        void shouldTellThePassengerWhenItWillOpen() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), TOO_FAR_AHEAD);

            Instant opens = TOO_FAR_AHEAD.minus(Duration.ofHours(24));

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString(opens.toString())));
        }

        @Test
        @DisplayName("should refuse half an hour before departure")
        void shouldRefuseHalfAnHourBeforeDeparture() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), ABOUT_TO_LEAVE);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title")
                            .value("Check-in has closed"));
        }

        /*
         * Closed and departed are different answers on purpose: one sends the
         * passenger to a desk, the other tells them the aeroplane has gone.
         */
        @Test
        @DisplayName("should say the flight has gone once it has")
        void shouldSayTheFlightHasGoneOnceItHas() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), ALREADY_GONE);

            mockMvc.perform(checkIn(booking))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title")
                            .value("The flight has departed"));
        }

        @Test
        @DisplayName("should issue nothing when the window is shut")
        void shouldIssueNothingWhenTheWindowIsShut() throws Exception {
            UUID booking = aBooking();
            givenBookingIsConfirmed(booking, aFlight(), ALREADY_GONE);

            mockMvc.perform(checkIn(booking));

            Assertions.assertThat(passCount()).isZero();
        }
    }

    @Nested
    @DisplayName("rejecting bad input")
    class BadInput {

        @Test
        @DisplayName("should reject a request that names no booking")
        void shouldRejectARequestThatNamesNoBooking() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(BOARDING_PASSES)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }

    private MockHttpServletRequestBuilder checkIn(final UUID bookingId) {
        String body = """
                {"bookingId": "%s"}
                """.formatted(bookingId);

        return MockMvcRequestBuilders.post(BOARDING_PASSES)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private void givenBookingIsConfirmed(final UUID bookingId,
                                         final UUID flightId,
                                         final Instant departure) {
        givenBookingIs(CONFIRMED, bookingId, flightId, departure);
    }

    private void givenBookingIs(final String status,
                                final UUID bookingId,
                                final UUID flightId,
                                final Instant departure) {
        BookingId booking = new BookingId(bookingId);
        FlightId flight = new FlightId(flightId);
        PassengerId passenger = new PassengerId(UUID.randomUUID());

        BookingToCheckIn answer = new BookingToCheckIn(booking, passenger, flight, status);
        BDDMockito.given(readBookingPort.byId(booking)).willReturn(answer);

        FlightSnapshot snapshot = new FlightSnapshot(
                flight, FLIGHT_NUMBER, ORIGIN, DESTINATION, departure);
        BDDMockito.given(readFlightPort.byId(flight)).willReturn(snapshot);
    }

    private void givenNoSuchBooking(final UUID bookingId) {
        BookingId booking = new BookingId(bookingId);

        BDDMockito.given(readBookingPort.byId(booking))
                .willThrow(new BookingNotFoundException(booking));
    }

    private long passCount() {
        return jdbcTemplate.queryForObject(COUNT_PASSES, Long.class);
    }

    private static String passIdFrom(final String body) {
        return JsonPath.read(body, "$.boardingPassId");
    }

    private static UUID aBooking() {
        return UUID.randomUUID();
    }

    private static UUID aFlight() {
        return UUID.randomUUID();
    }
}
