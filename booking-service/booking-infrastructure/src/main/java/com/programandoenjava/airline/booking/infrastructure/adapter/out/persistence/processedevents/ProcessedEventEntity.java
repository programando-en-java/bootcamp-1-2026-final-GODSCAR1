package com.programandoenjava.airline.booking.infrastructure.adapter.out.persistence.processedevents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
class ProcessedEventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    /*
     * Filled by the column's own default rather than written from here. It is
     * an audit column that nothing reads, and taking it from the database keeps
     * a clock out of an adapter that has no other use for one.
     */
    @Column(name = "processed_at", nullable = false, insertable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {
        // required by JPA
    }

    ProcessedEventEntity(final UUID eventId) {
        this.eventId = eventId;
    }
}
