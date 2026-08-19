package com.programandoenjava.airline.booking.application.port.out.savebooking;

import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.Booking;

import java.time.Instant;
import java.util.Optional;

public interface SaveBookingPort {

    /*
     * Empty means the key was already taken, and the booking that took it is
     * what comes back. Insert-and-see rather than look-then-insert: there is no
     * row to lock before this one exists, so a prior lookup would leave a window
     * between the question and the write.
     */
    Optional<Booking> saveIfNew(Booking booking, IdempotencyKey key);

    void updateStatus(Booking booking);

    /** Records that the seats a failed booking held have gone back. */
    void markSeatsReleased(Booking booking, Instant at);
}
