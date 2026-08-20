package com.programandoenjava.airline.checkin.application.port.in.checkin.exception;

import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;

public class BookingNotConfirmedException extends RuntimeException {

    public BookingNotConfirmedException(final BookingId bookingId, final String status) {
        super("Booking " + bookingId.value() + " is " + status
                + " and only a confirmed booking can be checked in");
    }
}
