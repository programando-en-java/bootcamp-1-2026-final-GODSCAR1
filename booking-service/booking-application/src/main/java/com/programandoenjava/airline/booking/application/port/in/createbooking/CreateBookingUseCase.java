package com.programandoenjava.airline.booking.application.port.in.createbooking;

import com.programandoenjava.airline.booking.domain.booking.Booking;

public interface CreateBookingUseCase {

    Booking create(CreateBookingCommand command);
}
