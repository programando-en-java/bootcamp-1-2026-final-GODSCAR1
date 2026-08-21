package com.programandoenjava.airline.notification.domain.notification;

import com.programandoenjava.airline.notification.domain.shared.DomainValidationException;

import java.util.UUID;

public record NotificationId(UUID value) {

    public static NotificationId newId() {
        return new NotificationId(UUID.randomUUID());
    }

    public NotificationId {
        if (value == null) {
            throw new DomainValidationException("A notification id is required");
        }
    }
}
