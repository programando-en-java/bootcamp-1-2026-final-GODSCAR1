package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingpass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface BoardingPassJpaRepository extends JpaRepository<BoardingPassEntity, UUID> {

    Optional<BoardingPassEntity> findByBookingId(UUID bookingId);

    /**
     * Native because ON CONFLICT has no JPQL spelling. A booking checked in
     * twice at once writes one pass, and the loser reads the winner's instead
     * of failing.
     */
    @Modifying
    @Query(value = """
            INSERT INTO boarding_passes (id, booking_id, passenger_id, flight_id,
                                         flight_number, origin, destination,
                                         departure_time, boarding_sequence, issued_at)
            VALUES (:id, :bookingId, :passengerId, :flightId,
                    :flightNumber, :origin, :destination,
                    :departureTime, :boardingSequence, :issuedAt)
            ON CONFLICT (booking_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("bookingId") UUID bookingId,
                       @Param("passengerId") UUID passengerId,
                       @Param("flightId") UUID flightId,
                       @Param("flightNumber") String flightNumber,
                       @Param("origin") String origin,
                       @Param("destination") String destination,
                       @Param("departureTime") Instant departureTime,
                       @Param("boardingSequence") int boardingSequence,
                       @Param("issuedAt") Instant issuedAt);
}
