package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
class OutboxEntity {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(nullable = false, length = 128)
    private String topic;

    /* Stored as jsonb: queryable if it ever needs to be, and validated on write. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEntity() {
        // required by JPA
    }

    OutboxEntity(final UUID id,
                 final String aggregateType,
                 final String aggregateId,
                 final String topic,
                 final String payload,
                 final Instant createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    String getAggregateId() {
        return aggregateId;
    }

    String getTopic() {
        return topic;
    }

    String getPayload() {
        return payload;
    }

    void markPublished(final Instant at) {
        this.publishedAt = at;
    }
}
