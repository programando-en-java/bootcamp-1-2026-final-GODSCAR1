package com.programandoenjava.airline.payment.application.port.in.paybooking.exception;

import com.programandoenjava.airline.payment.domain.payment.BookingId;

/** A booking already settled one way or the other cannot be charged again. */
public class BookingNotPayableException extends RuntimeException {

    public BookingNotPayableException(final BookingId bookingId, final String status) {
        super("Booking " + bookingId.value() + " is " + status + " and cannot be paid");
    }
}
