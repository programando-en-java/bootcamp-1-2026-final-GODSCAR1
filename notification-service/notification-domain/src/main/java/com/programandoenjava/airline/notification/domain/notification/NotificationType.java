package com.programandoenjava.airline.notification.domain.notification;

/**
 * What happened to the passenger's journey. One per story in EPIC-05, and the
 * reason a notification can be recognised later without reading its text.
 */
public enum NotificationType {

    BOOKING_CREATED,
    PAYMENT_SUCCEEDED,
    CHECK_IN_COMPLETED
}
