package com.programandoenjava.airline.flight.application.port.in;

import com.programandoenjava.airline.flight.application.port.shared.PageQuery;
import com.programandoenjava.airline.flight.application.port.shared.SortableField;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

@DisplayName("Page query")
class PageQueryTest {

    @Nested
    @DisplayName("when the size is within bounds")
    class ValidSize {

        @Test
        @DisplayName("should accept the maximum")
        void shouldAcceptTheMaximum() {
            PageQuery page = new PageQuery(0, PageQuery.MAX_SIZE, List.of());

            Assertions.assertThat(page.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("should accept a size below the maximum")
        void shouldAcceptASizeBelowTheMaximum() {
            PageQuery page = new PageQuery(0, 15, List.of());

            Assertions.assertThat(page.size()).isEqualTo(15);
        }

        @Test
        @DisplayName("should accept a single element page")
        void shouldAcceptASingleElementPage() {
            PageQuery page = new PageQuery(3, 1, List.of());

            Assertions.assertThat(page.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("when the request is out of bounds")
    class InvalidRequest {

        @Test
        @DisplayName("should reject a size above the maximum")
        void shouldRejectASizeAboveTheMaximum() {
            Assertions.assertThatThrownBy(() -> new PageQuery(0, 21, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not exceed 20");
        }

        @Test
        @DisplayName("should reject a size of zero")
        void shouldRejectASizeOfZero() {
            Assertions.assertThatThrownBy(() -> new PageQuery(0, 0, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject a negative page index")
        void shouldRejectANegativePageIndex() {
            Assertions.assertThatThrownBy(() -> new PageQuery(-1, 20, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }
    }

    @Nested
    @DisplayName("when checking whether it is sorted")
    class Sorting {

        @Test
        @DisplayName("should report an empty sort list as unsorted")
        void shouldReportAnEmptySortListAsUnsorted() {
            PageQuery page = new PageQuery(0, 20, List.of());

            Assertions.assertThat(page.isUnsorted()).isTrue();
        }

        @Test
        @DisplayName("should keep the sort list immutable")
        void shouldKeepTheSortListImmutable() {
            PageQuery.SortOrder byPrice =
                    new PageQuery.SortOrder(SortableField.PRICE, PageQuery.Direction.DESC);
            PageQuery page = new PageQuery(0, 20, List.of(byPrice));

            Assertions.assertThatThrownBy(() -> page.sort().add(byPrice))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
