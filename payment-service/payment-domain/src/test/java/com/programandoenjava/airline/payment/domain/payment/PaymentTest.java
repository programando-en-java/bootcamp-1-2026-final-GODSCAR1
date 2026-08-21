package com.programandoenjava.airline.payment.domain.payment;

import com.programandoenjava.airline.payment.domain.shared.DomainValidationException;
import com.programandoenjava.airline.payment.domain.shared.Money;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

@DisplayName("Payment")
class PaymentTest {

    private static final Instant PROCESSED_AT = Instant.parse("2026-03-10T12:00:00Z");
    private static final String AMOUNT = "500000.00";
    private static final String COP = "COP";
    private static final String CARD = "4242424242424242";

    @Test
    @DisplayName("should keep only the last four digits of the card")
    void shouldKeepOnlyTheLastFourDigitsOfTheCard() {
        Payment payment = aSucceededPayment();

        Assertions.assertThat(payment.cardLastFourDigits()).isEqualTo("4242");
    }

    @Test
    @DisplayName("should record a refusal as fully as a charge")
    void shouldRecordARefusalAsFullyAsACharge() {
        Payment payment = Payment.failed(PaymentId.newId(), aBooking(),
                Money.of(AMOUNT, COP), new CardNumber(CARD), PROCESSED_AT);

        Assertions.assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
        Assertions.assertThat(payment.cardLastFourDigits()).isEqualTo("4242");
        Assertions.assertThat(payment.amount()).isEqualTo(Money.of(AMOUNT, COP));
    }

    @Test
    @DisplayName("should tell a charge from a refusal")
    void shouldTellAChargeFromARefusal() {
        Payment charged = aSucceededPayment();
        Payment refused = Payment.failed(PaymentId.newId(), aBooking(),
                Money.of(AMOUNT, COP), new CardNumber(CARD), PROCESSED_AT);

        Assertions.assertThat(charged.hasSucceeded()).isTrue();
        Assertions.assertThat(refused.hasSucceeded()).isFalse();
    }

    @Test
    @DisplayName("should refuse to exist without the booking it paid for")
    void shouldRefuseToExistWithoutTheBookingItPaidFor() {
        PaymentId id = PaymentId.newId();
        Money amount = Money.of(AMOUNT, COP);
        CardNumber card = new CardNumber(CARD);

        Assertions.assertThatThrownBy(
                        () -> Payment.succeeded(id, null, amount, card, PROCESSED_AT))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must be for a booking");
    }

    private static Payment aSucceededPayment() {
        return Payment.succeeded(PaymentId.newId(), aBooking(),
                Money.of(AMOUNT, COP), new CardNumber(CARD), PROCESSED_AT);
    }

    private static BookingId aBooking() {
        UUID id = UUID.randomUUID();

        return new BookingId(id);
    }
}
