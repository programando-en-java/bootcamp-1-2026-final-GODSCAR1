package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.flight.SeatInventory;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Seat inventory")
class SeatInventoryTest {

    @Nested
    @DisplayName("when created")
    class Creation {

        @Test
        @DisplayName("should start with every seat for sale")
        void shouldStartWithEverySeatForSale() {
            SeatInventory inventory = SeatInventory.empty(120);

            Assertions.assertThat(inventory.available()).isEqualTo(120);
        }

        @Test
        @DisplayName("should reject a capacity of zero")
        void shouldRejectACapacityOfZero() {
            Assertions.assertThatThrownBy(() -> SeatInventory.empty(0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should reject more available seats than capacity")
        void shouldRejectMoreAvailableSeatsThanCapacity() {
            Assertions.assertThatThrownBy(() -> new SeatInventory(120, 121))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("between 0 and 120");
        }

        @Test
        @DisplayName("should reject a negative flightNumber of available seats")
        void shouldRejectANegativeNumberOfAvailableSeats() {
            Assertions.assertThatThrownBy(() -> new SeatInventory(120, -1))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("when blocking seats")
    class Blocking {

        @Test
        @DisplayName("should reduce availability without changing capacity")
        void shouldReduceAvailabilityWithoutChangingCapacity() {
            SeatInventory inventory = SeatInventory.empty(120);

            SeatInventory blocked = inventory.block(2);

            Assertions.assertThat(blocked.available()).isEqualTo(118);
            Assertions.assertThat(blocked.total()).isEqualTo(120);
        }

        @Test
        @DisplayName("should leave the original untouched")
        void shouldLeaveTheOriginalUntouched() {
            SeatInventory inventory = SeatInventory.empty(120);

            inventory.block(2);

            Assertions.assertThat(inventory.available()).isEqualTo(120);
        }

        @Test
        @DisplayName("should allow blocking the last seat")
        void shouldAllowBlockingTheLastSeat() {
            SeatInventory almostFull = new SeatInventory(120, 1);

            SeatInventory soldOut = almostFull.block(1);

            Assertions.assertThat(soldOut.hasAvailability()).isFalse();
        }

        @Test
        @DisplayName("should reject blocking more seats than are available")
        void shouldRejectBlockingMoreSeatsThanAreAvailable() {
            SeatInventory almostFull = new SeatInventory(120, 3);

            Assertions.assertThatThrownBy(() -> almostFull.block(4))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("only 3 available");
        }

        @Test
        @DisplayName("should reject blocking zero seats")
        void shouldRejectBlockingZeroSeats() {
            SeatInventory inventory = SeatInventory.empty(120);

            Assertions.assertThatThrownBy(() -> inventory.block(0))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("when releasing seats")
    class Releasing {

        @Test
        @DisplayName("should return them to availability")
        void shouldReturnThemToAvailability() {
            SeatInventory partiallyBooked = new SeatInventory(120, 100);

            SeatInventory released = partiallyBooked.release(2);

            Assertions.assertThat(released.available()).isEqualTo(102);
        }

        @Test
        @DisplayName("should undo a block exactly")
        void shouldUndoABlockExactly() {
            SeatInventory inventory = SeatInventory.empty(120);

            SeatInventory compensated = inventory.block(3).release(3);

            Assertions.assertThat(compensated).isEqualTo(inventory);
        }

        @Test
        @DisplayName("should refuse to release beyond capacity")
        void shouldRefuseToReleaseBeyondCapacity() {
            SeatInventory almostEmpty = new SeatInventory(120, 119);

            Assertions.assertThatThrownBy(() -> almostEmpty.release(2))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("exceed capacity");
        }

        @Test
        @DisplayName("should reject releasing zero seats")
        void shouldRejectReleasingZeroSeats() {
            SeatInventory inventory = new SeatInventory(120, 100);

            Assertions.assertThatThrownBy(() -> inventory.release(0))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}