package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.EnableDatabaseTest;
import com.programandoenjava.airline.flight.TestcontainersConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight.FlightPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock.SeatBlockPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.flight.infrastructure.transaction.TransactionSupportConfiguration;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

/**
 * Reading one flight, which is what checkin-service will do before it issues a
 * boarding pass.
 *
 * <p>No clock is pinned, unlike the other slices here. This endpoint does not
 * ask what time it is: a departed flight is as readable as any other, and the
 * test below says so on purpose rather than by omission.
 *
 * <p>Flights are looked up by number rather than by hard-coded id, so changing
 * a uuid in the seed does not turn these into an unexplained 404.
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
@DisplayName("Flight read slice (hand-built, no full context)")
class FlightReadSliceTest {

    private static final String FLIGHT = "/api/v1/flights/{flightId}";

    private static final String BOOKABLE = "AV8001";

    /** The one flight in the seed whose departure sits in the past. */
    private static final String DEPARTED = "AV9000";

    private static final String ORIGIN = "BOG";
    private static final String DESTINATION = "MDE";
    private static final String DEPARTURE = "2026-03-11T08:00:00Z";
    private static final String ARRIVAL = "2026-03-11T09:00:00Z";
    private static final int SEATS_LEFT = 45;
    private static final String CURRENCY = "COP";

    /*
     * Compared as a number with tolerance rather than for equality: Jackson
     * writes the BigDecimal as a JSON number and JsonPath reads it back as a
     * Double, so 250000.00 and 250000.0 are the same amount and different
     * objects.
     */
    private static final double FARE = 250_000.00;
    private static final double A_CENT = 0.001;

    private static final String FIND_FLIGHT_ID =
            "SELECT id::text FROM flights WHERE flight_number = ?";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("when the flight exists")
    class Existing {

        @Test
        @DisplayName("should answer with the flight it names")
        void shouldAnswerWithTheFlightItNames() throws Exception {
            String flightId = flightIdOf(BOOKABLE);

            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHT, flightId))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(flightId))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.flightNumber").value(BOOKABLE));
        }

        /*
         * The fields a boarding pass is printed from. Asserted together because
         * a pass missing any one of them is not a pass.
         */
        @Test
        @DisplayName("should carry the route and the schedule a pass is printed from")
        void shouldCarryTheRouteAndTheSchedule() throws Exception {
            String flightId = flightIdOf(BOOKABLE);

            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHT, flightId))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.origin").value(ORIGIN))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.destination").value(DESTINATION))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.departureTime").value(DEPARTURE))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.arrivalTime").value(ARRIVAL));
        }

        @Test
        @DisplayName("should report the seats and the fare")
        void shouldReportTheSeatsAndTheFare() throws Exception {
            String flightId = flightIdOf(BOOKABLE);

            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHT, flightId))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.availableSeats").value(SEATS_LEFT))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.price")
                            .value(Matchers.closeTo(FARE, A_CENT)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.currency").value(CURRENCY));
        }

        @Test
        @DisplayName("should keep the seat capacity to itself")
        void shouldKeepTheSeatCapacityToItself() throws Exception {
            String flightId = flightIdOf(BOOKABLE);

            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHT, flightId))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalSeats").doesNotExist());
        }

        /*
         * The search hides departed flights because nobody can buy a seat on
         * one. Reading is the opposite case: a passenger who has already flown
         * still has a pass, and checkin-service needs the departure time to say
         * why it will not issue another.
         */
        @Test
        @DisplayName("should answer for a flight that has already departed")
        void shouldAnswerForAFlightThatHasAlreadyDeparted() throws Exception {
            String flightId = flightIdOf(DEPARTED);

            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHT, flightId))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.flightNumber").value(DEPARTED));
        }
    }

    @Nested
    @DisplayName("when it does not")
    class Missing {

        @Test
        @DisplayName("should answer not found")
        void shouldAnswerNotFound() throws Exception {
            String unknown = UUID.randomUUID().toString();

            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHT, unknown))
                    .andExpect(MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("should say which flight it looked for")
        void shouldSayWhichFlightItLookedFor() throws Exception {
            String unknown = UUID.randomUUID().toString();

            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHT, unknown))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Flight not found"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString(unknown)));
        }
    }

    private String flightIdOf(final String flightNumber) {
        return jdbcTemplate.queryForObject(FIND_FLIGHT_ID, String.class, flightNumber);
    }
}
