package com.programandoenjava.airline.payment.domain.shared;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

@DisplayName("Money")
class MoneyTest {

    private static final String COP = "COP";

    @Test
    @DisplayName("should reject a charge for nothing")
    void shouldRejectAChargeForNothing() {
        Assertions.assertThatThrownBy(() -> Money.of("0", COP))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    @DisplayName("should reject a negative charge")
    void shouldRejectANegativeCharge() {
        Assertions.assertThatThrownBy(() -> Money.of("-1.00", COP))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    @DisplayName("should pad to the currency's fraction digits")
    void shouldPadToTheCurrencysFractionDigits() {
        Money money = Money.of("500000.5", COP);

        BigDecimal expected = new BigDecimal("500000.50");
        Assertions.assertThat(money.amount()).isEqualTo(expected);
    }

    @Test
    @DisplayName("should refuse to round an amount the currency cannot hold")
    void shouldRefuseToRoundAnAmountTheCurrencyCannotHold() {
        Assertions.assertThatThrownBy(() -> Money.of("500000.567", COP))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("precision");
    }

    @Test
    @DisplayName("should not treat the same amount in different currencies as equal")
    void shouldNotTreatTheSameAmountInDifferentCurrenciesAsEqual() {
        Money pesos = Money.of("100.00", COP);
        Money dollars = Money.of("100.00", "USD");

        Assertions.assertThat(pesos).isNotEqualTo(dollars);
    }
}
