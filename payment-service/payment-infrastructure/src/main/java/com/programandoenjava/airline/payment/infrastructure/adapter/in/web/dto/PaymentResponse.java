package com.programandoenjava.airline.payment.infrastructure.adapter.in.web.dto;

import com.programandoenjava.airline.payment.domain.payment.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID paymentId,
                              UUID bookingId,
                              BigDecimal amount,
                              String currency,
                              String status,
                              String cardLastFourDigits,
                              Instant processedAt) {

    public static PaymentResponse from(final Payment payment) {
        String currency = payment.amount().currency().getCurrencyCode();
        String status = payment.status().name();

        return new PaymentResponse(
                payment.id().value(),
                payment.bookingId().value(),
                payment.amount().amount(),
                currency,
                status,
                payment.cardLastFourDigits(),
                payment.processedAt());
    }
}
