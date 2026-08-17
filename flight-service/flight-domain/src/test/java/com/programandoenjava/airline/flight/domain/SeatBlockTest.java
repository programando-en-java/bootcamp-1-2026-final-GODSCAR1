package com.programandoenjava.airline.flight.domain;

import com.programandoenjava.airline.flight.domain.flight.FlightId;
import com.programandoenjava.airline.flight.domain.seatblock.BookingId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlock;
import com.programandoenjava.airline.flight.domain.seatblock.SeatBlockId;
import com.programandoenjava.airline.flight.domain.seatblock.SeatCount;
import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

@DisplayName("Seat block")
class SeatBlockTest {

    private static final Instant BLOCKED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    @DisplayName("should hold the seats it was created with")
    void shouldHoldTheSeatsItWasCreatedWith() {
        SeatBlock block = aBlock();

        Assertions.assertThat(block.seats().value()).isEqualTo(2);
    }

    @Test
    @DisplayName("should refuse to exist without a flight")
    void shouldRefuseToExistWithoutAFlight() {
        SeatBlockId id = SeatBlockId.newId();
        BookingId booking = new BookingId(UUID.randomUUID());
        SeatCount two = new SeatCount(2);

        Assertions.assertThatThrownBy(() -> new SeatBlock(id, null, booking, two, BLOCKED_AT))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must belong to a flight");
    }

    @Test
    @DisplayName("should refuse to exist without a booking")
    void shouldRefuseToExistWithoutABooking() {
        SeatBlockId id = SeatBlockId.newId();
        FlightId flight = FlightId.newId();
        SeatCount two = new SeatCount(2);

        Assertions.assertThatThrownBy(() -> new SeatBlock(id, flight, null, two, BLOCKED_AT))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must belong to a booking");
    }

    @Test
    @DisplayName("should refuse to exist without a time")
    void shouldRefuseToExistWithoutATime() {
        SeatBlockId id = SeatBlockId.newId();
        FlightId flight = FlightId.newId();
        BookingId booking = new BookingId(UUID.randomUUID());
        SeatCount two = new SeatCount(2);

        Assertions.assertThatThrownBy(() -> new SeatBlock(id, flight, booking, two, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("when it was taken");
    }

    private static SeatBlock aBlock() {
        return new SeatBlock(
                SeatBlockId.newId(),
                FlightId.newId(),
                new BookingId(UUID.randomUUID()),
                new SeatCount(2),
                BLOCKED_AT);
    }
}