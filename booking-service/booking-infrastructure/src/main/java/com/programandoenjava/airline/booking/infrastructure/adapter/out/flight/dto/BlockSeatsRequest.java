package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight.dto;

import java.util.UUID;

public record BlockSeatsRequest(UUID bookingId, int seats) {
}
