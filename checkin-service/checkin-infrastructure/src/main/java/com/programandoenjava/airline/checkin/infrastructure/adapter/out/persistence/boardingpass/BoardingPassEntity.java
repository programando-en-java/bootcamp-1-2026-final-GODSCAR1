package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "boarding_passes")
class BoardingPassEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "passenger_id", nullable = false)
    private UUID passengerId;

    @Column(name = "flight_id", nullable = false)
    private UUID flightId;

    @Column(name = "flight_number", nullable = false, length = 16)
    private String flightNumber;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "boarding_sequence", nullable = false)
    private int boardingSequence;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected BoardingPassEntity() {
    }

    BoardingPassEntity(final UUID id,
                       final UUID bookingId,
                       final UUID passengerId,
                       final UUID flightId,
                       final String flightNumber,
                       final String origin,
                       final String destination,
                       final Instant departureTime,
                       final int boardingSequence,
                       final Instant issuedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.passengerId = passengerId;
        this.flightId = flightId;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.boardingSequence = boardingSequence;
        this.issuedAt = issuedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getBookingId() {
        return bookingId;
    }

    UUID getPassengerId() {
        return passengerId;
    }

    UUID getFlightId() {
        return flightId;
    }

    String getFlightNumber() {
        return flightNumber;
    }

    String getOrigin() {
        return origin;
    }

    String getDestination() {
        return destination;
    }

    Instant getDepartureTime() {
        return departureTime;
    }

    int getBoardingSequence() {
        return boardingSequence;
    }

    Instant getIssuedAt() {
        return issuedAt;
    }
}
