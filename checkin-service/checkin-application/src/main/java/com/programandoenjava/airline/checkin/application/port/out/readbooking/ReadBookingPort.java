package com.programandoenjava.airline.checkin.application.port.out.readbooking;

import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;

public interface ReadBookingPort {

    BookingToCheckIn byId(BookingId bookingId);
}
