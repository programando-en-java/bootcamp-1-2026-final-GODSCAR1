package com.programandoenjava.airline.checkin.infrastructure.adapter.out.booking.dto;

import java.util.UUID;

public record BookingResponse(UUID bookingId,
                              UUID passengerId,
                              UUID flightId,
                              String status) {
}
