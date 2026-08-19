package com.programandoenjava.airline.payment.application.port.out.readbooking;

import com.programandoenjava.airline.payment.domain.payment.BookingId;

public interface ReadBookingPort {

    BookingToPay byId(BookingId bookingId);
}
