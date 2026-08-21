package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "passenger_id", nullable = false)
    private UUID passengerId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationTypeEntity type;

    @Column(nullable = false, length = 128)
    private String subject;

    @Column(nullable = false, length = 1024)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private @Nullable Instant sentAt;

    protected NotificationEntity() {
    }

    NotificationEntity(final UUID id,
                       final UUID passengerId,
                       final UUID bookingId,
                       final NotificationTypeEntity type,
                       final String subject,
                       final String body,
                       final Instant createdAt) {
        this.id = id;
        this.passengerId = passengerId;
        this.bookingId = bookingId;
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt;
    }

    void markSent(final Instant when) {
        sentAt = when;
    }
}
