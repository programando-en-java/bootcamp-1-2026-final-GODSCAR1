package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking;

import com.programandoenjava.airline.booking.domain.booking.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
class BookingEntity {

    @Id
    private UUID id;

    @Column(name = "passenger_id", nullable = false)
    private UUID passengerId;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "seat_block_id", nullable = false)
    private UUID seatBlockId;

    @Column(nullable = false)
    private int seats;

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_currency", nullable = false, length = 3)
    private String totalCurrency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BookingStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "seats_released_at")
    private Instant seatsReleasedAt;

    protected BookingEntity() {
        // required by JPA
    }

    BookingEntity(final UUID id,
                  final UUID passengerId,
                  final UUID flightId,
                  final UUID seatBlockId,
                  final int seats,
                  final BigDecimal priceAmount,
                  final String priceCurrency,
                  final BigDecimal totalAmount,
                  final String totalCurrency,
                  final BookingStatus status,
                  final String idempotencyKey,
                  final Instant createdAt) {
        this.id = id;
        this.passengerId = passengerId;
        this.flightId = flightId;
        this.seatBlockId = seatBlockId;
        this.seats = seats;
        this.priceAmount = priceAmount;
        this.priceCurrency = priceCurrency;
        this.totalAmount = totalAmount;
        this.totalCurrency = totalCurrency;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getPassengerId() {
        return passengerId;
    }

    UUID getFlightId() {
        return flightId;
    }

    UUID getSeatBlockId() {
        return seatBlockId;
    }

    int getSeats() {
        return seats;
    }

    BigDecimal getPriceAmount() {
        return priceAmount;
    }

    String getPriceCurrency() {
        return priceCurrency;
    }

    BigDecimal getTotalAmount() {
        return totalAmount;
    }

    String getTotalCurrency() {
        return totalCurrency;
    }

    BookingStatus getStatus() {
        return status;
    }

    String getIdempotencyKey() {
        return idempotencyKey;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void settleAs(final BookingStatus newStatus) {
        this.status = newStatus;
    }

    void recordSeatsReleased(final Instant at) {
        this.seatsReleasedAt = at;
    }
}
