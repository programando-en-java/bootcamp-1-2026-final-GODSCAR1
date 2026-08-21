package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.payment;

import com.programandoenjava.airline.payment.domain.payment.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
class PaymentEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "card_last_four_digits", nullable = false, length = 4)
    private String cardLastFourDigits;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected PaymentEntity() {
    }

    PaymentEntity(final UUID id,
                  final UUID bookingId,
                  final BigDecimal amount,
                  final String currency,
                  final PaymentStatus status,
                  final String cardLastFourDigits,
                  final Instant processedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.cardLastFourDigits = cardLastFourDigits;
        this.processedAt = processedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getBookingId() {
        return bookingId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getCurrency() {
        return currency;
    }

    PaymentStatus getStatus() {
        return status;
    }

    String getCardLastFourDigits() {
        return cardLastFourDigits;
    }

    Instant getProcessedAt() {
        return processedAt;
    }
}
