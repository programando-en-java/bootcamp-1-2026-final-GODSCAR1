package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.seatblock.SeatCount;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Seat count")
class SeatCountTest {

    @Nested
    @DisplayName("when the count is within what one booking may hold")
    class Accepted {

        @Test
        @DisplayName("should accept a single seat")
        void shouldAcceptASingleSeat() {
            SeatCount one = new SeatCount(1);

            Assertions.assertThat(one.value()).isEqualTo(1);
        }

        @Test
        @DisplayName("should accept the largest booking allowed")
        void shouldAcceptTheLargestBookingAllowed() {
            SeatCount nine = new SeatCount(SeatCount.MAX);

            Assertions.assertThat(nine.value()).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("when the count is outside what one booking may hold")
    class Rejected {

        @Test
        @DisplayName("should reject a booking for no seats at all")
        void shouldRejectABookingForNoSeatsAtAll() {
            Assertions.assertThatThrownBy(() -> new SeatCount(0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("At least one seat");
        }

        @Test
        @DisplayName("should reject a negative count")
        void shouldRejectANegativeCount() {
            Assertions.assertThatThrownBy(() -> new SeatCount(-1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("At least one seat");
        }

        @Test
        @DisplayName("should reject a party larger than one booking allows")
        void shouldRejectAPartyLargerThanOneBookingAllows() {
            Assertions.assertThatThrownBy(() -> new SeatCount(SeatCount.MAX + 1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot exceed 9 seats");
        }
    }
}