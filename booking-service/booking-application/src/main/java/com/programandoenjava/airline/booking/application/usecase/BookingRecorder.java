package com.programandoenjava.airline.booking.application.usecase;

import com.programandoenjava.airline.booking.application.event.BookingCreated;
import com.programandoenjava.airline.booking.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.application.transaction.UnitOfWork;
import com.programandoenjava.airline.booking.domain.booking.Booking;

import java.util.Optional;

/**
 * The write and the announcement, in one transaction. A class of its own rather
 * than a method on CreateBookingService, because {@code @UnitOfWork} is
 * proxy-based and does nothing when a bean calls itself. Keeping it here also
 * keeps the transaction off the seat hold, which is a network call.
 */
public class BookingRecorder {

    private final SaveBookingPort saveBooking;
    private final DomainEventPublisher events;

    public BookingRecorder(final SaveBookingPort saveBooking,
                           final DomainEventPublisher events) {
        this.saveBooking = saveBooking;
        this.events = events;
    }

    /*
     * Announced only when this call is the one that wrote it. A repeated
     * request carrying the same idempotency key gets the booking that already
     * exists, and nothing new has happened to tell anyone about.
     */
    @UnitOfWork
    public Booking record(final Booking booking, final IdempotencyKey key) {
        Optional<Booking> lostTheRace = saveBooking.saveIfNew(booking, key);
        if (lostTheRace.isPresent()) {
            return lostTheRace.get();
        }

        events.publish(new BookingCreated(booking));

        return booking;
    }
}
