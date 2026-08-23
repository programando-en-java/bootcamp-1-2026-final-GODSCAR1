package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.flight.EnableDatabaseTest;
import com.programandoenjava.airline.flight.TestcontainersConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight.FlightPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock.SeatBlockPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.config.ApplicationConfiguration;
import com.programandoenjava.airline.flight.infrastructure.security.SecurityConfiguration;
import com.programandoenjava.airline.flight.infrastructure.transaction.TransactionSupportConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.convention.TestBean;
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

/* The only test that fails if the header, the property or the filter go: every
 * other one here carries a user's token or asks for something public. */
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
        JacksonAutoConfiguration.class
})
@AutoConfigureMockMvc
@Sql(scripts = "/db/testdata/R__seed_flights.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("Calling as a service rather than as a person")
class ServiceTokenSliceTest {

    private static final String SEAT_BLOCKS = "/api/v1/flights/{flightId}/seat-blocks";
    private static final String SEAT_BLOCK = "/api/v1/flights/{flightId}/seat-blocks/{seatBlockId}";
    private static final String ONE_FLIGHT = "/api/v1/flights/{flightId}";

    private static final String HEADER = "X-Service-Token";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private static final String KNOWN = "a-token-for-tests";

    private static final String BOOKABLE = "AV8001";
    private static final int SEATS_WANTED = 2;

    private static final String FIND_FLIGHT_ID =
            "SELECT id::text FROM flights WHERE flight_number = ?";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    /* The seeded flights depart in March 2026, so without this they have all
     * left and every hold is refused. */
    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("with the secret it was given")
    class Known {

        @Test
        @DisplayName("should let it hold seats")
        void shouldLetItHoldSeats() throws Exception {
            mockMvc.perform(block().header(HEADER, KNOWN))
                    .andExpect(MockMvcResultMatchers.status().isCreated());
        }

        @Test
        @DisplayName("should let it give them back")
        void shouldLetItGiveThemBack() throws Exception {
            String seatBlockId = holdSeats();

            mockMvc.perform(MockMvcRequestBuilders
                            .delete(SEAT_BLOCK, flightId(), seatBlockId)
                            .header(HEADER, KNOWN))
                    .andExpect(MockMvcResultMatchers.status().isNoContent());
        }
    }

    @Nested
    @DisplayName("without it")
    class Unknown {

        @Test
        @DisplayName("should refuse a caller carrying nothing")
        void shouldRefuseACallerCarryingNothing() throws Exception {
            mockMvc.perform(block())
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("should refuse a caller carrying the wrong secret")
        void shouldRefuseACallerCarryingTheWrongSecret() throws Exception {
            mockMvc.perform(block().header(HEADER, "not the secret"))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized());
        }

        @Test
        @DisplayName("should authenticate nobody outside seat blocks")
        void shouldAuthenticateNobodyOutsideSeatBlocks() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders
                            .get(ONE_FLIGHT, flightId())
                            .header(HEADER, KNOWN))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        }
    }

    private MockHttpServletRequestBuilder block() {
        String body = """
                {"bookingId": "%s", "seats": %d}
                """.formatted(UUID.randomUUID(), SEATS_WANTED);

        return MockMvcRequestBuilders.post(SEAT_BLOCKS, flightId())
                .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String holdSeats() throws Exception {
        String body = mockMvc.perform(block().header(HEADER, KNOWN))
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.seatBlockId");
    }

    private String flightId() {
        return jdbcTemplate.queryForObject(FIND_FLIGHT_ID, String.class, BOOKABLE);
    }
}
