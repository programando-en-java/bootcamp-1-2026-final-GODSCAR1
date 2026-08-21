package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.processedevents;

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

    @Column(name = "processed_at", nullable = false, insertable = false, updatable = false)
    /* Filled by the column's own default, which keeps a clock out of this adapter. */
    private Instant processedAt;

    protected ProcessedEventEntity() {
    }

    ProcessedEventEntity(final UUID eventId) {
        this.eventId = eventId;
    }
}
