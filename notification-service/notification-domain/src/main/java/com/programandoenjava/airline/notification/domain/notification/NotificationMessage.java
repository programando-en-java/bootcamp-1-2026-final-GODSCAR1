package com.programandoenjava.airline.notification.domain.notification;

import com.programandoenjava.airline.notification.domain.shared.DomainValidationException;

import java.math.BigDecimal;
import java.time.Instant;

public record NotificationMessage(String subject, String body) {

    public NotificationMessage {
        if (subject == null || subject.isBlank()) {
            throw new DomainValidationException("A notification needs a subject");
        }
        if (body == null || body.isBlank()) {
            throw new DomainValidationException("A notification needs a body");
        }
    }

    public static NotificationMessage bookingCreated(final int seats,
                                                     final BigDecimal total,
                                                     final String currency) {
        String subject = "We have your booking";
        String body = "We are holding %d seat(s) for you. The total is %s %s, and your booking is confirmed once payment goes through."
                .formatted(seats, total.toPlainString(), currency);

        return new NotificationMessage(subject, body);
    }

    public static NotificationMessage paymentSucceeded(final BigDecimal amount,
                                                       final String currency) {
        String subject = "Your payment went through";
        String body = "We received %s %s. Your booking is confirmed and you can check in from a day before departure."
                .formatted(amount.toPlainString(), currency);

        return new NotificationMessage(subject, body);
    }

    public static NotificationMessage checkInCompleted(final String flightNumber,
                                                       final String origin,
                                                       final String destination,
                                                       final Instant departure,
                                                       final int boardingSequence) {
        String subject = "You are checked in for " + flightNumber;
        String body = "%s departs %s for %s at %s. You are number %d to board."
                .formatted(flightNumber, origin, destination, departure, boardingSequence);

        return new NotificationMessage(subject, body);
    }
}
