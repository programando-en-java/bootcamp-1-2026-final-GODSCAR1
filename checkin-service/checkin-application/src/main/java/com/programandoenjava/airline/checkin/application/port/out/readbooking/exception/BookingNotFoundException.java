package com.programandoenjava.airline.checkin.application.port.out.readbooking.exception;

import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(final BookingId bookingId) {
        super("No booking with id " + bookingId.value());
    }
}
