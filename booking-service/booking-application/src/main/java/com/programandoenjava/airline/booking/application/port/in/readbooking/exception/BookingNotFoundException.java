package com.programandoenjava.airline.booking.application.port.in.readbooking.exception;

import com.programandoenjava.airline.booking.domain.booking.BookingId;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(final BookingId bookingId) {
        super("No booking with id " + bookingId.value());
    }
}
