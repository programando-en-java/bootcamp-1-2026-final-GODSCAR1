package com.programandoenjava.airline.booking.application.port.out.findbooking;

import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.Booking;

import java.util.Optional;

public interface FindBookingPort {

    Optional<Booking> byIdempotencyKey(IdempotencyKey key);
}
