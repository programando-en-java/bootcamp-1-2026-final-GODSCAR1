package com.programandoenjava.airline.booking.application.usecase;

import com.programandoenjava.airline.booking.application.port.in.readbooking.ReadBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.booking.application.port.out.findbooking.FindBookingPort;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.domain.booking.BookingId;

public class ReadBookingService implements ReadBookingUseCase {

    private final FindBookingPort findBooking;

    public ReadBookingService(final FindBookingPort findBooking) {
        this.findBooking = findBooking;
    }

    @Override
    public Booking byId(final BookingId bookingId) {
        return findBooking.byId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }
}
