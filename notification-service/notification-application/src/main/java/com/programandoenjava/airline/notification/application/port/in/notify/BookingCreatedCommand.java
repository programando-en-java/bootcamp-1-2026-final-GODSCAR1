package com.programandoenjava.airline.notification.application.port.in.notify;

import com.programandoenjava.airline.notification.domain.notification.BookingId;
import com.programandoenjava.airline.notification.domain.notification.PassengerId;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingCreatedCommand(UUID eventId,
                                    PassengerId passengerId,
                                    BookingId bookingId,
                                    int seats,
                                    BigDecimal total,
                                    String currency) {
}
