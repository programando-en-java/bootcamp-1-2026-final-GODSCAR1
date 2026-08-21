package com.programandoenjava.airline.flight.infrastructure.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.programandoenjava.airline.flight.EnableDatabaseTest;
import com.programandoenjava.airline.flight.TestcontainersConfiguration;
import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight.FlightPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock.SeatBlockPersistenceConfiguration;
import com.programandoenjava.airline.flight.infrastructure.config.ApplicationConfiguration;
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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

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
@DisplayName("Flight search slice (hand-built, no full context)")
class FlightSearchSliceTest {

    private static final String FLIGHTS = "/api/v1/flights";

    private static final String ORIGIN = "origin";
    private static final String DESTINATION = "destination";
    private static final String DATE = "date";
    private static final String PAGE = "page";
    private static final String SIZE = "size";
    private static final String SORT = "sort";

    private static final String BOGOTA = "BOG";
    private static final String MEDELLIN = "MDE";

    private static final String FIRST_DAY = "2026-03-11";
    private static final String SECOND_DAY = "2026-03-12";
    private static final String PAST_DAY = "2026-03-09";

    private static final int BOOKABLE_FLIGHTS = 6;
    private static final int ON_THE_BOGOTA_MEDELLIN_ROUTE = 4;
    private static final int LEAVING_ON_THE_FIRST_DAY = 4;
    private static final int NONE = 0;
    private static final int ONE = 1;

    private static final int PAGE_SIZE = 2;
    private static final int FIRST_PAGE = 0;
    private static final int SECOND_PAGE = 1;
    private static final int OVER_THE_PAGE_LIMIT = PageQuery.MAX_SIZE + 1;

    private static final String DEAREST_FIRST = "price,desc";
    private static final String LATEST_ARRIVAL_FIRST = "arrivalTime,desc";
    private static final String AN_INTERNAL_COLUMN = "priceCurrency,asc";

    private static final String NOT_AN_AIRPORT = "BOGOTA";
    private static final String NOT_A_DATE = "no-soy-una-fecha";

    private static final String FLIGHT_NUMBERS = "$.content[*].flightNumber";
    private static final String ORIGINS = "$.content[*].origin";
    private static final String DESTINATIONS = "$.content[*].destination";
    private static final String TOTAL_ELEMENTS = "$.totalElements";
    private static final String DETAIL = "$.detail";

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
                    .andExpect(MockMvcResultMatchers.jsonPath(TOTAL_ELEMENTS)
                            .value(BOOKABLE_FLIGHTS))
                    .andExpect(MockMvcResultMatchers.jsonPath(FLIGHT_NUMBERS)
                            .value(Matchers.contains(
                                    "AV8001", "AV8002", "LA8100", "LA8101", "AV8003", "AV8004")));
        }

        @Test
        @DisplayName("should leave out the flight that already departed")
        void shouldLeaveOutADepartedFlight() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS))
                    .andExpect(MockMvcResultMatchers.jsonPath(FLIGHT_NUMBERS)
                            .value(Matchers.not(Matchers.hasItem("AV9000"))));
        }

        @Test
        @DisplayName("should leave out the flight with no seats left")
        void shouldLeaveOutASoldOutFlight() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS))
                    .andExpect(MockMvcResultMatchers.jsonPath(FLIGHT_NUMBERS)
                            .value(Matchers.not(Matchers.hasItem("AV9001"))));
        }

        @Test
        @DisplayName("should report the seats a passenger can still buy")
        void shouldReportRemainingSeats() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(ORIGIN, BOGOTA).param(DESTINATION, MEDELLIN))
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
                            .param(ORIGIN, BOGOTA).param(DESTINATION, MEDELLIN))
                    .andExpect(MockMvcResultMatchers.jsonPath(ORIGINS)
                            .value(Matchers.everyItem(Matchers.is(BOGOTA))))
                    .andExpect(MockMvcResultMatchers.jsonPath(DESTINATIONS)
                            .value(Matchers.everyItem(Matchers.is(MEDELLIN))))
                    .andExpect(MockMvcResultMatchers.jsonPath(TOTAL_ELEMENTS)
                            .value(ON_THE_BOGOTA_MEDELLIN_ROUTE));
        }

        @Test
        @DisplayName("should not care how the passenger spells the airport")
        void shouldAcceptLowerCase() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(ORIGIN, "bog").param(DESTINATION, "mde"))
                    .andExpect(MockMvcResultMatchers.jsonPath(ORIGINS)
                            .value(Matchers.everyItem(Matchers.is(BOGOTA))))
                    .andExpect(MockMvcResultMatchers.jsonPath(TOTAL_ELEMENTS)
                            .value(ON_THE_BOGOTA_MEDELLIN_ROUTE));
        }

        @Test
        @DisplayName("should answer everything leaving an airport when only the origin is given")
        void shouldNarrowByOriginAlone() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param(ORIGIN, BOGOTA))
                    .andExpect(MockMvcResultMatchers.jsonPath(ORIGINS)
                            .value(Matchers.everyItem(Matchers.is(BOGOTA))))
                    .andExpect(MockMvcResultMatchers.jsonPath(FLIGHT_NUMBERS)
                            .value(Matchers.hasItem("LA8100")));
        }

        @Test
        @DisplayName("should answer everything arriving at an airport when only the destination is given")
        void shouldNarrowByDestinationAlone() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param(DESTINATION, MEDELLIN))
                    .andExpect(MockMvcResultMatchers.jsonPath(DESTINATIONS)
                            .value(Matchers.everyItem(Matchers.is(MEDELLIN))))
                    .andExpect(MockMvcResultMatchers.jsonPath(FLIGHT_NUMBERS)
                            .value(Matchers.hasItem("LA8101")));
        }

        @Test
        @DisplayName("should answer nothing for a route nobody flies")
        void shouldAnswerNothingForAnUnservedRoute() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(ORIGIN, MEDELLIN).param(DESTINATION, BOGOTA))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(TOTAL_ELEMENTS).value(NONE));
        }
    }

    @Nested
    @DisplayName("narrowing by day")
    class ByDate {

        @Test
        @DisplayName("should answer with the flights leaving that day only")
        void shouldNarrowToOneDay() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param(DATE, FIRST_DAY))
                    .andExpect(MockMvcResultMatchers.jsonPath(TOTAL_ELEMENTS)
                            .value(LEAVING_ON_THE_FIRST_DAY))
                    .andExpect(MockMvcResultMatchers.jsonPath(FLIGHT_NUMBERS)
                            .value(Matchers.contains("AV8001", "AV8002", "LA8100", "LA8101")));
        }

        @Test
        @DisplayName("should combine the day with the route")
        void shouldCombineDayAndRoute() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(ORIGIN, BOGOTA).param(DESTINATION, MEDELLIN)
                            .param(DATE, SECOND_DAY))
                    .andExpect(MockMvcResultMatchers.jsonPath(TOTAL_ELEMENTS).value(ONE))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].flightNumber")
                            .value("AV8003"));
        }

        @Test
        @DisplayName("should answer nothing for a day already gone")
        void shouldAnswerNothingForAPastDay() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param(DATE, PAST_DAY))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath(TOTAL_ELEMENTS).value(NONE));
        }
    }

    @Nested
    @DisplayName("paging and ordering")
    class PagingAndOrdering {

        @Test
        @DisplayName("should hand back one page while reporting the whole count")
        void shouldPage() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(PAGE, String.valueOf(FIRST_PAGE))
                            .param(SIZE, String.valueOf(PAGE_SIZE)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.content",
                            Matchers.hasSize(PAGE_SIZE)))
                    .andExpect(MockMvcResultMatchers.jsonPath(TOTAL_ELEMENTS)
                            .value(BOOKABLE_FLIGHTS))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(PAGE_SIZE));
        }

        @Test
        @DisplayName("should carry on where the previous page stopped")
        void shouldContinueOnTheNextPage() throws Exception {
            String firstPage = mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(PAGE, String.valueOf(FIRST_PAGE))
                            .param(SIZE, String.valueOf(PAGE_SIZE)))
                    .andReturn().getResponse().getContentAsString();
            String secondPage = mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(PAGE, String.valueOf(SECOND_PAGE))
                            .param(SIZE, String.valueOf(PAGE_SIZE)))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.page").value(SECOND_PAGE))
                    .andReturn().getResponse().getContentAsString();

            List<String> first = JsonPath.read(firstPage, FLIGHT_NUMBERS);
            List<String> second = JsonPath.read(secondPage, FLIGHT_NUMBERS);

            Assertions.assertThat(second)
                    .hasSize(PAGE_SIZE)
                    .doesNotContainAnyElementsOf(first);
        }

        @Test
        @DisplayName("should order by fare when the passenger asks for the dearest first")
        void shouldOrderByPriceDescending() throws Exception {
            String body = mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(SORT, DEAREST_FIRST))
                    .andReturn().getResponse().getContentAsString();

            List<Double> prices = JsonPath.read(body, "$.content[*].price");

            Assertions.assertThat(prices)
                    .hasSize(BOOKABLE_FLIGHTS)
                    .isSortedAccordingTo(Comparator.reverseOrder());
        }

        @Test
        @DisplayName("should order by arrival when asked")
        void shouldOrderByArrival() throws Exception {
            String body = mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(ORIGIN, BOGOTA).param(DESTINATION, MEDELLIN)
                            .param(SORT, LATEST_ARRIVAL_FIRST))
                    .andReturn().getResponse().getContentAsString();

            List<String> arrivals = JsonPath.read(body, "$.content[*].arrivalTime");

            Assertions.assertThat(arrivals).isSortedAccordingTo(Comparator.reverseOrder());
        }
    }

    @Nested
    @DisplayName("rejecting bad input")
    class RejectingBadInput {

        @Test
        @DisplayName("should reject an airport that is not a three letter code")
        void shouldRejectMalformedAirport() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param(ORIGIN, NOT_AN_AIRPORT))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(DETAIL)
                            .value(Matchers.containsString("three letters")));
        }

        @Test
        @DisplayName("should reject a date that is not a date")
        void shouldRejectMalformedDate() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param(DATE, NOT_A_DATE))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }

        @Test
        @DisplayName("should refuse to hand over more than a page at a time")
        void shouldRejectAnOversizedPage() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS)
                            .param(SIZE, String.valueOf(OVER_THE_PAGE_LIMIT)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(DETAIL)
                            .value(Matchers.containsString(
                                    "must not exceed " + PageQuery.MAX_SIZE)));
        }

        @Test
        @DisplayName("should refuse to order by a column that is not part of the contract")
        void shouldRejectSortingByAnInternalColumn() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(FLIGHTS).param(SORT, AN_INTERNAL_COLUMN))
                    .andExpect(MockMvcResultMatchers.jsonPath(DETAIL)
                            .value(Matchers.containsString("Unknown sort field")))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());
        }
    }
}