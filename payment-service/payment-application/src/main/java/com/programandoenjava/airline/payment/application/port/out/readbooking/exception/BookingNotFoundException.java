package com.programandoenjava.airline.payment.application.port.out.readbooking.exception;

import com.programandoenjava.airline.payment.domain.payment.BookingId;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(final BookingId bookingId) {
        super("No booking with id " + bookingId.value());
    }
}
