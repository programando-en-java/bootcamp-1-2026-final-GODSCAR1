package com.programandoenjava.airline.booking.domain.booking;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Seat count")
class SeatCountTest {

    @Test
    @DisplayName("should accept the largest party one booking may hold")
    void shouldAcceptTheLargestPartyOneBookingMayHold() {
        SeatCount nine = new SeatCount(SeatCount.MAX);

        Assertions.assertThat(nine.value()).isEqualTo(9);
    }

    @Test
    @DisplayName("should reject a party larger than one booking allows")
    void shouldRejectAPartyLargerThanOneBookingAllows() {
        int overTheLimit = SeatCount.MAX + 1;

        Assertions.assertThatThrownBy(() -> new SeatCount(overTheLimit))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("cannot exceed 9 seats");
    }

    @Test
    @DisplayName("should reject a booking for no seats at all")
    void shouldRejectABookingForNoSeatsAtAll() {
        Assertions.assertThatThrownBy(() -> new SeatCount(0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("At least one seat");
    }
}