package com.programandoenjava.airline.booking.application.port.out.savebooking;

import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.Booking;

import java.time.Instant;
import java.util.Optional;

public interface SaveBookingPort {

    /* Empty means this call wrote it. A booking means the key was already taken. */
    Optional<Booking> saveIfNew(Booking booking, IdempotencyKey key);

    void updateStatus(Booking booking);

    void markSeatsReleased(Booking booking, Instant at);
}
