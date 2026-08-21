package com.programandoenjava.airline.flight.domain.flight;

import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;
import com.programandoenjava.airline.flight.domain.flight.exception.InsufficientSeatsException;

public record SeatInventory(int total, int available) {

    public SeatInventory {
        if (total <= 0) {
            throw new DomainValidationException("Total seats must be positive, was: " + total);
        }
        if (available < 0 || available > total) {
            throw new DomainValidationException(
                    "Available seats must be between 0 and " + total + ", was: " + available);
        }
    }

    public static SeatInventory empty(final int total) {
        return new SeatInventory(total, total);
    }

    public boolean hasAvailability() {
        return available > 0;
    }

    public boolean canAccommodate(final int seats) {
        return seats > 0 && seats <= available;
    }

    public SeatInventory block(final int seats) {
        if (!canAccommodate(seats)) {
            throw new InsufficientSeatsException(
                    "Cannot block " + seats + " seats, only " + available + " available");
        }
        return new SeatInventory(total, available - seats);
    }

    public SeatInventory release(final int seats) {
        if (seats <= 0) {
            throw new DomainValidationException("Seats to release must be positive, was: " + seats);
        }
        if (available + seats > total) {
            throw new DomainValidationException(
                    "Releasing " + seats + " seats would exceed capacity " + total);
        }
        return new SeatInventory(total, available + seats);
    }
}
