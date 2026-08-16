package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.programandoenjava.airline.flight.EnableDatabaseTest;
import com.programandoenjava.airline.flight.TestcontainersConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.PersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.config.ApplicationConfiguration;
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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@SpringBootTest(classes = {
        FlightController.class,
        GlobalExceptionHandler.class,
        ApplicationConfiguration.class,
        PersistenceConfiguration.class
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
@DisplayName("Flight search slice (hand-built, no full context)")
class FlightSearchSliceTest {

    private static final String FLIGHTS = "/api/v1/flights";


    private static final String FIRST_DAY = "2026-03-11";
    private static final String SECOND_DAY = "2026-03-12";
    private static final String PAST_DAY = "2026-03-09";

    @Autowired
    private MockMvc mockMvc;

    @TestBean
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("browsing the catalogue")
    class Browsing {

        @Test
        @DisplayName("should answer with every bookable flight, soonest first")
        void shouldListTheBookableCatalogue() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(6))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.contains(
                                    "AV8001", "AV8002", "LA8100", "LA8101", "AV8003", "AV8004")));
        }

        @Test
        @DisplayName("should leave out the flight that already departed")
        void shouldLeaveOutADepartedFlight() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.not(Matchers.hasItem("AV9000"))));
        }

        @Test
        @DisplayName("should leave out the flight with no seats left")
        void shouldLeaveOutASoldOutFlight() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.not(Matchers.hasItem("AV9001"))));
        }

        @Test
        @DisplayName("should report the seats a passenger can still buy")
        void shouldReportRemainingSeats() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("origin", "BOG").param("destination", "MDE"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].flightNumber")
                            .value("AV8001"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].availableSeats")
                            .value(45))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].currency")
                            .value("COP"));
        }

        @Test
        @DisplayName("should keep the seat capacity to itself")
        void shouldNotExposeCapacity() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].totalSeats")
                            .doesNotExist());
        }
    }

    @Nested
    @DisplayName("narrowing by route")
    class ByRoute {

        @Test
        @DisplayName("should answer with the flights on that route only")
        void shouldNarrowToOneRoute() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("origin", "BOG").param("destination", "MDE"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(4))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.contains("AV8001", "AV8002", "AV8003", "AV8004")));
        }

        @Test
        @DisplayName("should not care how the passenger spells the airport")
        void shouldAcceptLowerCase() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("origin", "bog").param("destination", "mde"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(4));
        }

        @Test
        @DisplayName("should answer everything leaving an airport when only the origin is given")
        void shouldNarrowByOriginAlone() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param("origin", "BOG"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(5))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.hasItem("LA8100")));
        }

        @Test
        @DisplayName("should answer everything arriving at an airport when only the destination is given")
        void shouldNarrowByDestinationAlone() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param("destination", "MDE"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(5))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.hasItem("LA8101")));
        }

        @Test
        @DisplayName("should answer nothing for a route nobody flies")
        void shouldAnswerNothingForAnUnservedRoute() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("origin", "MDE").param("destination", "BOG"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("narrowing by day")
    class ByDate {

        @Test
        @DisplayName("should answer with the flights leaving that day only")
        void shouldNarrowToOneDay() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param("date", FIRST_DAY))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(4))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.contains("AV8001", "AV8002", "LA8100", "LA8101")));
        }

        @Test
        @DisplayName("should combine the day with the route")
        void shouldCombineDayAndRoute() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("origin", "BOG").param("destination", "MDE")
                            .param("date", SECOND_DAY))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].flightNumber")
                            .value("AV8003"));
        }

        @Test
        @DisplayName("should answer nothing for a day already gone")
        void shouldAnswerNothingForAPastDay() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param("date", PAST_DAY))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("paging and ordering")
    class PagingAndOrdering {

        @Test
        @DisplayName("should hand back one page while reporting the whole count")
        void shouldPage() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("page", "0").param("size", "2"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(2)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.totalElements").value(6))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(2));
        }

        @Test
        @DisplayName("should carry on where the previous page stopped")
        void shouldContinueOnTheNextPage() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("page", "1").param("size", "2"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.page").value(1))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.contains("LA8100", "LA8101")));
        }

        @Test
        @DisplayName("should order by fare when the passenger asks for the dearest first")
        void shouldOrderByPriceDescending() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("sort", "price,desc"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[*].flightNumber")
                            .value(Matchers.contains(
                                    "LA8100", "AV8002", "LA8101", "AV8004", "AV8001", "AV8003")));
        }

        @Test
        @DisplayName("should order by arrival when asked")
        void shouldOrderByArrival() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param("origin", "BOG").param("destination", "MDE")
                            .param("sort", "arrivalTime,desc"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].flightNumber")
                            .value("AV8004"));
        }
    }

    @Nested
    @DisplayName("rejecting bad input")
    class RejectingBadInput {

        @Test
        @DisplayName("should reject an airport that is not a three letter code")
        void shouldRejectMalformedAirport() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param("origin", "BOGOTA"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString("three letters")));
        }

        @Test
        @DisplayName("should reject a date that is not a date")
        void shouldRejectMalformedDate() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param("date", "no-soy-una-fecha"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should refuse to hand over more than a page at a time")
        void shouldRejectAnOversizedPage() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param("size", "100"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString("must not exceed 20")));
        }

        @Test
        @DisplayName("should refuse to order by a column that is not part of the contract")
        void shouldRejectSortingByAnInternalColumn() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param("sort", "priceCurrency,asc"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.detail")
                            .value(Matchers.containsString("Unknown sort field")));
        }
    }
}