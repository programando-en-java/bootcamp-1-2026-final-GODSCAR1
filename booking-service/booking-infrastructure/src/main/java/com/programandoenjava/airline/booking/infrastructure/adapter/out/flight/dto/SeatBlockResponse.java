package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SeatBlockResponse(UUID seatBlockId,
                                UUID flightId,
                                UUID bookingId,
                                int seats,
                                BigDecimal pricePerSeat,
                                String currency,
                                Instant blockedAt) {
}
