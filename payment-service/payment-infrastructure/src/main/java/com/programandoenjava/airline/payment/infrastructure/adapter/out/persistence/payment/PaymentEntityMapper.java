package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.payment;

import com.programandoenjava.airline.payment.domain.payment.BookingId;
import com.programandoenjava.airline.payment.domain.payment.Payment;
import com.programandoenjava.airline.payment.domain.payment.PaymentId;
import com.programandoenjava.airline.payment.domain.shared.Money;

import java.util.Currency;

final class PaymentEntityMapper {

    private PaymentEntityMapper() {
    }

    static PaymentEntity toEntity(final Payment payment) {
        String currency = payment.amount().currency().getCurrencyCode();

        return new PaymentEntity(
                payment.id().value(),
                payment.bookingId().value(),
                payment.amount().amount(),
                currency,
                payment.status(),
                payment.cardLastFourDigits(),
                payment.processedAt());
    }

    static Payment toDomain(final PaymentEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        Money amount = new Money(entity.getAmount(), currency);
        PaymentId id = new PaymentId(entity.getId());
        BookingId bookingId = new BookingId(entity.getBookingId());

        return new Payment(id, bookingId, amount, entity.getStatus(),
                entity.getCardLastFourDigits(), entity.getProcessedAt());
    }
}
