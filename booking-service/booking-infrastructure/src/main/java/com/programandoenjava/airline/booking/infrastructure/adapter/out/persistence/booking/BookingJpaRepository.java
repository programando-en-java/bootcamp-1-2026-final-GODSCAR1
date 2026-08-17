package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = """
            INSERT INTO bookings (id, passenger_id, flight_id, seat_block_id, seats,
                                  price_amount, price_currency,
                                  total_amount, total_currency,
                                  status, idempotency_key, created_at)
            VALUES (:id, :passengerId, :flightId, :seatBlockId, :seats,
                    :priceAmount, :priceCurrency,
                    :totalAmount, :totalCurrency,
                    :status, :idempotencyKey, :createdAt)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("passengerId") UUID passengerId,
                       @Param("flightId") UUID flightId,
                       @Param("seatBlockId") UUID seatBlockId,
                       @Param("seats") int seats,
                       @Param("priceAmount") BigDecimal priceAmount,
                       @Param("priceCurrency") String priceCurrency,
                       @Param("totalAmount") BigDecimal totalAmount,
                       @Param("totalCurrency") String totalCurrency,
                       @Param("status") String status,
                       @Param("idempotencyKey") String idempotencyKey,
                       @Param("createdAt") Instant createdAt);
}
