package com.programandoenjava.airline.booking.infrastructure.adapter.in.web;

import com.programandoenjava.airline.booking.application.port.in.createbooking.CreateBookingCommand;
import com.programandoenjava.airline.booking.application.port.shared.IdempotencyKey;
import com.programandoenjava.airline.booking.domain.booking.FlightId;
import com.programandoenjava.airline.booking.domain.booking.PassengerId;
import com.programandoenjava.airline.booking.domain.booking.SeatCount;
import com.programandoenjava.airline.booking.infrastructure.adapter.in.web.dto.CreateBookingRequest;

import java.util.UUID;

final class CreateBookingRequestMapper {

    private CreateBookingRequestMapper() {
    }

    static CreateBookingCommand toCommand(final UUID passengerId,
                                          final String idempotencyKey,
                                          final CreateBookingRequest request) {
        PassengerId passenger = new PassengerId(passengerId);
        FlightId flight = new FlightId(request.flightId());
        SeatCount seats = new SeatCount(request.seats());
        IdempotencyKey key = new IdempotencyKey(idempotencyKey);

        return new CreateBookingCommand(passenger, flight, seats, key);
    }
}
