package com.programandoenjava.airline.booking.application.port.in.readbooking;

import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.domain.booking.BookingId;

public interface ReadBookingUseCase {

    Booking byId(BookingId bookingId);
}
