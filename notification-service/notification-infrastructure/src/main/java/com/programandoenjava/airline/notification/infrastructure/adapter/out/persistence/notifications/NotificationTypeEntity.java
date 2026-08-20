package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.notifications;

/**
 * The domain enum written again for the database, so that renaming a constant
 * in the domain cannot silently change what is already stored (ADR-003).
 */
enum NotificationTypeEntity {

    BOOKING_CREATED,
    PAYMENT_SUCCEEDED,
    CHECK_IN_COMPLETED
}
