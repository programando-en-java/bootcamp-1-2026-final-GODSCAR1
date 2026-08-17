package com.programandoenjava.airline.flight.application.port.in.blockseats;

import com.programandoenjava.airline.flight.domain.seatblock.BookingId;

public class BookingAlreadyHoldsSeatsException extends RuntimeException {

    public BookingAlreadyHoldsSeatsException(BookingId bookingId) {
        super("Booking " + bookingId.value() + " already holds seats");
    }
}