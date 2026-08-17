package com.programandoenjava.airline.flight.application.usecase;

import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.application.port.shared.PageResult;
import com.programandoenjava.airline.flight.application.port.in.searchflights.SearchFlightsQuery;
import com.programandoenjava.airline.flight.application.port.shared.SortableField;
import com.programandoenjava.airline.flight.application.port.out.searchflights.FlightSearchCriteria;
import com.programandoenjava.airline.flight.application.port.out.searchflights.LoadFlightsPort;
import com.programandoenjava.airline.flight.domain.shared.AirportCode;
import com.programandoenjava.airline.flight.domain.flight.Flight;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/*
 * Only the search window is asserted here. Everything else this class does is
 * moving fields into a DTO, and a test over that breaks on any refactor without
 * catching a single real defect.
 */

@ExtendWith(MockitoExtension.class)
@DisplayName("Search flights")
class SearchFlightsServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final AirportCode BOG = new AirportCode("BOG");
    private static final AirportCode MDE = new AirportCode("MDE");

    @Mock
    private LoadFlightsPort loadFlightsPort;

    @Captor
    private ArgumentCaptor<FlightSearchCriteria> criteriaCaptor;

    private SearchFlightsService service;

    @BeforeEach
    void setUp() {
        service = new SearchFlightsService(loadFlightsPort, FIXED_CLOCK);
        PageResult<Flight> emptyPage =
                new PageResult<>(List.of(), 0, 20, 0);
        BDDMockito.given(loadFlightsPort.search(BDDMockito.any())).willReturn(emptyPage);
    }

    @Nested
    @DisplayName("when no date is given")
    class WithoutDate {

        @Test
        @DisplayName("should search from the current instant onwards with no upper bound")
        void shouldSearchFromTheCurrentInstantOnwardsWithNoUpperBound() {
            SearchFlightsQuery query = searchWithDate(null);

            service.search(query);

            FlightSearchCriteria criteria = capturedCriteria();
            Assertions.assertThat(criteria.departingFrom()).isEqualTo(NOW);
            Assertions.assertThat(criteria.departingBefore()).isNull();
        }
    }

    @Nested
    @DisplayName("when a date is given")
    class WithDate {

        @Test
        @DisplayName("should cover the whole day for a future date")
        void shouldCoverTheWholeDayForAFutureDate() {
            SearchFlightsQuery query = searchWithDate(LocalDate.parse("2026-09-03"));

            service.search(query);

            FlightSearchCriteria criteria = capturedCriteria();
            Assertions.assertThat(criteria.departingFrom())
                    .isEqualTo(Instant.parse("2026-09-03T00:00:00Z"));
            Assertions.assertThat(criteria.departingBefore())
                    .isEqualTo(Instant.parse("2026-09-04T00:00:00Z"));
        }

        @Test
        @DisplayName("should start at the current instant when the date is today")
        void shouldStartAtTheCurrentInstantWhenTheDateIsToday() {
            SearchFlightsQuery query = searchWithDate(LocalDate.parse("2026-09-01"));

            service.search(query);

            FlightSearchCriteria criteria = capturedCriteria();
            Assertions.assertThat(criteria.departingFrom()).isEqualTo(NOW);
            Assertions.assertThat(criteria.departingBefore())
                    .isEqualTo(Instant.parse("2026-09-02T00:00:00Z"));
        }

        @Test
        @DisplayName("should collapse the window when the date is in the past")
        void shouldCollapseTheWindowWhenTheDateIsInThePast() {
            SearchFlightsQuery query = searchWithDate(LocalDate.parse("2026-08-31"));

            service.search(query);

            FlightSearchCriteria criteria = capturedCriteria();
            Assertions.assertThat(criteria.isEmptyWindow()).isTrue();
        }
    }

    private FlightSearchCriteria capturedCriteria() {
        BDDMockito.then(loadFlightsPort).should().search(criteriaCaptor.capture());
        return criteriaCaptor.getValue();
    }

    private static SearchFlightsQuery searchWithDate(final LocalDate date) {
        PageQuery page = new PageQuery(0, 20, List.of(
                new PageQuery.SortOrder(SortableField.DEPARTURE_TIME, PageQuery.Direction.ASC)));
        return new SearchFlightsQuery(BOG, MDE, date, page);
    }
}