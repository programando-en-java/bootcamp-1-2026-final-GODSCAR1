package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "flights")
class FlightEntity {

    @Id
    private UUID id;

    @Column(name = "flight_number", nullable = false)
    private String flightNumber;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency;

    protected FlightEntity() {
        // required by JPA
    }

    UUID getId() {
        return id;
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

    Instant getArrivalTime() {
        return arrivalTime;
    }

    int getTotalSeats() {
        return totalSeats;
    }

    int getAvailableSeats() {
        return availableSeats;
    }

    BigDecimal getPriceAmount() {
        return priceAmount;
    }

    String getPriceCurrency() {
        return priceCurrency;
    }
}
