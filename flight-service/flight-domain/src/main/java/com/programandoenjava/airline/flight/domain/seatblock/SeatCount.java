package com.programandoenjava.airline.flight.domain.seatblock;

import com.programandoenjava.airline.flight.domain.shared.DomainValidationException;

public record SeatCount(int value) {

    public static final int MAX = 9;

    public SeatCount {
        if (value < 1) {
            throw new DomainValidationException("At least one seat must be requested");
        }
        if (value > MAX) {
            throw new DomainValidationException(
                    "A single booking cannot exceed " + MAX + " seats, requested " + value);
        }
    }
}
