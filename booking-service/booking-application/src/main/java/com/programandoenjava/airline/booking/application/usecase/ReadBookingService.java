package com.programandoenjava.airline.booking.application.usecase;

import com.programandoenjava.airline.booking.application.port.in.readbooking.ReadBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.booking.application.port.out.findbooking.FindBookingPort;
import com.programandoenjava.airline.booking.application.port.shared.Caller;
import com.programandoenjava.airline.booking.domain.booking.Booking;
import com.programandoenjava.airline.booking.domain.booking.BookingId;

public class ReadBookingService implements ReadBookingUseCase {

    private final FindBookingPort findBooking;

    public ReadBookingService(final FindBookingPort findBooking) {
        this.findBooking = findBooking;
    }

    /**
     * A booking that is not yours is answered as one that does not exist.
     *
     * <p>Deliberately not a 403: refusing tells the caller the booking is real,
     * and an endpoint that says which ids exist is a list of other people's
     * bookings waiting to be walked (ADR-022).
     */
    @Override
    public Booking byId(final BookingId bookingId, final Caller caller) {
        Booking booking = findBooking.byId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        boolean maySeeIt = caller.isStaff() || caller.is(booking.passengerId().value());

        if (!maySeeIt) {
            throw new BookingNotFoundException(bookingId);
        }

        return booking;
    }
}
