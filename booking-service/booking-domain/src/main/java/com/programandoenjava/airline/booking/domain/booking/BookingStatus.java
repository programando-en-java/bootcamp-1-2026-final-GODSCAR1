package com.programandoenjava.airline.booking.domain.booking;

/**
 * Where a booking stands. EXPIRED is still absent: nobody has said how long a
 * hold lasts, so it would be a guess in a way the other two no longer are.
 */
public enum BookingStatus {

    PENDING,
    CONFIRMED,
    FAILED
}
