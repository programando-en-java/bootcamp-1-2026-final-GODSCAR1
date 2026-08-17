package com.programandoenjava.airline.booking.domain.shared;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

@DisplayName("Money")
class MoneyTest {

    private static final String COP = "COP";
    private static final String USD = "USD";
    private static final String JPY = "JPY";

    @Nested
    @DisplayName("when normalising the scale")
    class Scale {

        @Test
        @DisplayName("should pad to the currency's fraction digits")
        void shouldPadToTheCurrencysFractionDigits() {
            Money money = Money.of("250000.5", COP);

            BigDecimal expected = new BigDecimal("250000.50");
            Assertions.assertThat(money.amount()).isEqualTo(expected);
        }

        @Test
        @DisplayName("should treat two spellings of the same amount as equal")
        void shouldTreatTwoSpellingsOfTheSameAmountAsEqual() {
            Money withOneDecimal = Money.of("250000.0", COP);
            Money withTwoDecimals = Money.of("250000.00", COP);

            Assertions.assertThat(withOneDecimal).isEqualTo(withTwoDecimals);
        }

        @Test
        @DisplayName("should accept a whole amount for a currency without decimals")
        void shouldAcceptAWholeAmountForACurrencyWithoutDecimals() {
            Money money = Money.of("1500", JPY);

            BigDecimal expected = new BigDecimal("1500");
            Assertions.assertThat(money.amount()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("when the amount carries more precision than the currency allows")
    class ExcessPrecision {

        @Test
        @DisplayName("should refuse to round silently")
        void shouldRefuseToRoundSilently() {
            Assertions.assertThatThrownBy(() -> Money.of("250000.567", COP))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("precision");
        }

        @Test
        @DisplayName("should refuse decimals on a currency that has none")
        void shouldRefuseDecimalsOnACurrencyThatHasNone() {
            Assertions.assertThatThrownBy(() -> Money.of("1500.50", JPY))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("when the amount is invalid")
    class InvalidAmount {

        @Test
        @DisplayName("should reject a negative amount")
        void shouldRejectANegativeAmount() {
            Assertions.assertThatThrownBy(() -> Money.of("-1.00", COP))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("should accept zero")
        void shouldAcceptZero() {
            Money free = Money.of("0", COP);

            Assertions.assertThat(free.amount().signum()).isZero();
        }

        @Test
        @DisplayName("should reject a missing currency")
        void shouldRejectAMissingCurrency() {
            BigDecimal amount = new BigDecimal("100.00");

            Assertions.assertThatThrownBy(() -> new Money(amount, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("currency");
        }
    }

    @Nested
    @DisplayName("when multiplied by a seat count")
    class Multiplication {

        @Test
        @DisplayName("should give the fare for every seat")
        void shouldGiveTheFareForEverySeat() {
            Money fare = Money.of("250000.00", COP);

            Money forThree = fare.times(3);

            Money expected = Money.of("750000.00", COP);
            Assertions.assertThat(forThree).isEqualTo(expected);
        }

        @Test
        @DisplayName("should keep the currency it started in")
        void shouldKeepTheCurrencyItStartedIn() {
            Money fare = Money.of("250000.00", COP);

            Money forThree = fare.times(3);

            Currency expected = Currency.getInstance(COP);
            Assertions.assertThat(forThree.currency()).isEqualTo(expected);
        }

        @Test
        @DisplayName("should refuse to multiply by nothing")
        void shouldRefuseToMultiplyByNothing() {
            Money fare = Money.of("250000.00", COP);

            Assertions.assertThatThrownBy(() -> fare.times(0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should not treat the same amount in different currencies as equal")
        void shouldNotTreatTheSameAmountInDifferentCurrenciesAsEqual() {
            Money pesos = Money.of("100.00", COP);
            Money dollars = Money.of("100.00", USD);

            Assertions.assertThat(pesos).isNotEqualTo(dollars);
        }
    }
}