package com.programandoenjava.airline.notification.application.port.in.notify;

import com.programandoenjava.airline.notification.domain.notification.BookingId;
import com.programandoenjava.airline.notification.domain.notification.PassengerId;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What booking.created.v1 carries, as this service needs it. The flight is not
 * here: booking-service holds seats by id and never learns the flight number,
 * so there would be nothing to put in the message.
 */
public record BookingCreatedCommand(UUID eventId,
                                    PassengerId passengerId,
                                    BookingId bookingId,
                                    int seats,
                                    BigDecimal total,
                                    String currency) {
}
