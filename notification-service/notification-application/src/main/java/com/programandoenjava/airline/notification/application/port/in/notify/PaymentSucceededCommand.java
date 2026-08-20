package com.programandoenjava.airline.notification.application.port.in.notify;

import com.programandoenjava.airline.notification.domain.notification.BookingId;
import com.programandoenjava.airline.notification.domain.notification.PassengerId;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSucceededCommand(UUID eventId,
                                      PassengerId passengerId,
                                      BookingId bookingId,
                                      BigDecimal amount,
                                      String currency) {
}
