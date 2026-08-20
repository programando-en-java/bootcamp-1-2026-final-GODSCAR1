package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.processedevents;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface ProcessedEventsJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {

    /**
     * Claims an event, or does nothing if someone already has.
     *
     * <p>Native, because ON CONFLICT is what makes the check and the write one
     * statement. Two deliveries of the same message can arrive at once, and a
     * lookup followed by an insert would let both through.
     *
     * @return 1 if this call claimed it, 0 if it was already taken
     */
    @Modifying
    @Query(value = """
            INSERT INTO processed_events (event_id, processed_at)
            VALUES (:eventId, now())
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int claim(@Param("eventId") UUID eventId);
}
