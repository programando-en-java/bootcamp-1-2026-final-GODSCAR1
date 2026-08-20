package com.programandoenjava.airline.checkin.infrastructure.adapter.out.booking.dto;

import java.util.UUID;

/**
 * Only the fields check-in reads. booking-service sends the fare and the seat
 * count too; ignoring them here means a field added there does not break
 * anything in this service.
 */
public record BookingResponse(UUID bookingId,
                              UUID passengerId,
                              UUID flightId,
                              String status) {
}
