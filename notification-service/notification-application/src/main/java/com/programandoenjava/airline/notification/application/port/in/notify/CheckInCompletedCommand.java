package com.programandoenjava.airline.notification.application.port.in.notify;

import com.programandoenjava.airline.notification.domain.notification.BookingId;
import com.programandoenjava.airline.notification.domain.notification.PassengerId;

import java.time.Instant;
import java.util.UUID;

public record CheckInCompletedCommand(UUID eventId,
                                      PassengerId passengerId,
                                      BookingId bookingId,
                                      String flightNumber,
                                      String origin,
                                      String destination,
                                      Instant departureTime,
                                      int boardingSequence) {
}
