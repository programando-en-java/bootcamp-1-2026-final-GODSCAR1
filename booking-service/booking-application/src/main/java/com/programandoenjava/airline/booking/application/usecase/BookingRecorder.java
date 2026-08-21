package com.programandoenjava.airline.booking.application.usecase;

import com.programandoenjava.airline.booking.application.event.BookingCreated;
import com.programandoenjava.airline.booking.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.application.transaction.Isolation;
import com.programandoenjava.airline.booking.application.transaction.UnitOfWork;
import com.programandoenjava.airline.booking.domain.booking.Booking;

import java.util.Optional;

/* A class of its own because @UnitOfWork is proxy-based and does nothing when a
 * bean calls itself, and because it keeps the transaction off the seat hold. */
public class BookingRecorder {

    private final SaveBookingPort saveBooking;
    private final DomainEventPublisher events;

    public BookingRecorder(final SaveBookingPort saveBooking,
                           final DomainEventPublisher events) {
        this.saveBooking = saveBooking;
        this.events = events;
    }

    @UnitOfWork(isolation = Isolation.SERIALIZABLE)
    public Booking record(final Booking booking, final IdempotencyKey key) {
        Optional<Booking> lostTheRace = saveBooking.saveIfNew(booking, key);
        if (lostTheRace.isPresent()) {
            return lostTheRace.get();
        }

        events.publish(new BookingCreated(booking));

        return booking;
    }
}
