package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.seatblock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Seats held for a booking, as a row.
 *
 * <p>The flight is referenced by id rather than by association. A hold belongs
 * to a different aggregate than the flight (ADR-008), and a {@code @ManyToOne}
 * would invite loading one through the other, which is the coupling keeping them
 * apart is meant to avoid.
 */
@Entity
@Table(name = "seat_blocks")
class SeatBlockEntity {

    @Id
    private UUID id;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private int seats;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    protected SeatBlockEntity() {
        // required by JPA
    }

    SeatBlockEntity(final UUID id, final UUID flightId, final UUID bookingId, final int seats,
                    final String idempotencyKey, final Instant blockedAt) {
        this.id = id;
        this.flightId = flightId;
        this.bookingId = bookingId;
        this.seats = seats;
        this.idempotencyKey = idempotencyKey;
        this.blockedAt = blockedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getFlightId() {
        return flightId;
    }

    UUID getBookingId() {
        return bookingId;
    }

    int getSeats() {
        return seats;
    }

    String getIdempotencyKey() {
        return idempotencyKey;
    }

    Instant getBlockedAt() {
        return blockedAt;
    }
}