package com.programandoenjava.airline.booking.domain.booking;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;
import com.programandoenjava.airline.booking.domain.shared.Money;

import java.time.Instant;

public record Booking(
        BookingId id,
        PassengerId passengerId,
        FlightId flightId,
        SeatBlockId seatBlockId,
        SeatCount seats,
        Money pricePerSeat,
        Money total,
        BookingStatus status,
        Instant createdAt) {

    public Booking {
        if (id == null) {
            throw new DomainValidationException("A booking id is required");
        }
        if (passengerId == null) {
            throw new DomainValidationException("A booking must belong to a passenger");
        }
        if (flightId == null) {
            throw new DomainValidationException("A booking must be for a flight");
        }
        if (seatBlockId == null) {
            throw new DomainValidationException("A booking must hold seats");
        }
        if (seats == null) {
            throw new DomainValidationException("A booking must be for a number of seats");
        }
        if (pricePerSeat == null || total == null) {
            throw new DomainValidationException("A booking must carry a fare and a total");
        }
        if (status == null) {
            throw new DomainValidationException("A booking must have a status");
        }
        if (createdAt == null) {
            throw new DomainValidationException("A booking must record when it was made");
        }

        int seatCount = seats.value();
        Money expected = pricePerSeat.times(seatCount);
        if (!expected.equals(total)) {
            String message = "Total " + total.amount() + " does not match "
                    + seatCount + " seats at " + pricePerSeat.amount();
            throw new DomainValidationException(message);
        }
    }

    public static Booking of(final BookingId id,
                             final PassengerId passengerId,
                             final FlightId flightId,
                             final SeatBlockId seatBlockId,
                             final SeatCount seats,
                             final Money pricePerSeat,
                             final Instant now) {
        int seatCount = seats.value();
        Money total = pricePerSeat.times(seatCount);

        return new Booking(id, passengerId, flightId, seatBlockId, seats,
                pricePerSeat, total, BookingStatus.PENDING, now);
    }

    public Booking confirm() {
        if (status == BookingStatus.CONFIRMED) {
            return this;
        }
        if (status == BookingStatus.FAILED) {
            throw new DomainValidationException(
                    "Booking " + id.value() + " failed and cannot be confirmed");
        }

        return withStatus(BookingStatus.CONFIRMED);
    }

    public Booking fail() {
        if (status == BookingStatus.FAILED) {
            return this;
        }
        if (status == BookingStatus.CONFIRMED) {
            throw new DomainValidationException(
                    "Booking " + id.value() + " is confirmed and cannot be failed");
        }

        return withStatus(BookingStatus.FAILED);
    }

    public boolean isSettled() {
        return status != BookingStatus.PENDING;
    }

    private Booking withStatus(final BookingStatus newStatus) {
        return new Booking(id, passengerId, flightId, seatBlockId, seats,
                pricePerSeat, total, newStatus, createdAt);
    }
}
