package com.programandoenjava.airline.booking.domain.booking;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;

public record SeatCount(int value) {

    public static final int MAX = 9;

    public SeatCount {
        if (value < 1) {
            throw new DomainValidationException("At least one seat must be booked");
        }
        if (value > MAX) {
            String message = "A single booking cannot exceed " + MAX
                    + " seats, requested " + value;
            throw new DomainValidationException(message);
        }
    }
}
