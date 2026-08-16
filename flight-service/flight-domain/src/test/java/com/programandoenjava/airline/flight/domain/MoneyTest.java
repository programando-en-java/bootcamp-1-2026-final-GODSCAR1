package com.programandoenjava.airline.flight.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

@DisplayName("Money")
class MoneyTest {

    private static final Currency COP = Currency.getInstance("COP");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Nested
    @DisplayName("when normalising the scale")
    class Scale {

        @Test
        @DisplayName("should pad to the currency's fraction digits")
        void shouldPadToTheCurrencysFractionDigits() {
            Money money = Money.of("250000.5", "COP");

            Assertions.assertThat(money.amount()).isEqualTo(new BigDecimal("250000.50"));
        }

        @Test
        @DisplayName("should treat two spellings of the same amount as equal")
        void shouldTreatTwoSpellingsOfTheSameAmountAsEqual() {
            Money withOneDecimal = Money.of("250000.0", "COP");
            Money withTwoDecimals = Money.of("250000.00", "COP");

            Assertions.assertThat(withOneDecimal).isEqualTo(withTwoDecimals);
        }

        @Test
        @DisplayName("should accept a whole amount for a currency without decimals")
        void shouldAcceptAWholeAmountForACurrencyWithoutDecimals() {
            Money money = Money.of("1500", "JPY");

            Assertions.assertThat(money.amount()).isEqualTo(new BigDecimal("1500"));
        }
    }

    @Nested
    @DisplayName("when the amount carries more precision than the currency allows")
    class ExcessPrecision {

        @Test
        @DisplayName("should refuse to round silently")
        void shouldRefuseToRoundSilently() {
            Assertions.assertThatThrownBy(() -> Money.of("250000.567", "COP"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("precision");
        }

        @Test
        @DisplayName("should refuse decimals on a currency that has none")
        void shouldRefuseDecimalsOnACurrencyThatHasNone() {
            Assertions.assertThatThrownBy(() -> Money.of("1500.50", "JPY"))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("when the amount is invalid")
    class InvalidAmount {

        @Test
        @DisplayName("should reject a negative amount")
        void shouldRejectANegativeAmount() {
            Assertions.assertThatThrownBy(() -> Money.of("-1.00", "COP"))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("should accept zero")
        void shouldAcceptZero() {
            Money free = Money.of("0", "COP");

            Assertions.assertThat(free.amount().signum()).isZero();
        }

        @Test
        @DisplayName("should reject a missing currency")
        void shouldRejectAMissingCurrency() {
            BigDecimal amount = new BigDecimal("100.00");

            Assertions.assertThatThrownBy(() -> new Money(amount, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Currency");
        }
    }

    @Nested
    @DisplayName("when operating on two amounts")
    class Arithmetic {

        @Test
        @DisplayName("should add amounts in the same currency")
        void shouldAddAmountsInTheSameCurrency() {
            Money first = Money.of("100.00", "COP");
            Money second = Money.of("50.50", "COP");

            Money total = first.plus(second);

            Assertions.assertThat(total).isEqualTo(Money.of("150.50", "COP"));
        }

        @Test
        @DisplayName("should refuse to add different currencies")
        void shouldRefuseToAddDifferentCurrencies() {
            Money pesos = Money.of("100.00", "COP");
            Money dollars = Money.of("100.00", "USD");

            Assertions.assertThatThrownBy(() -> pesos.plus(dollars))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("should multiply by a passenger count")
        void shouldMultiplyByAPassengerCount() {
            Money fare = Money.of("250000.00", "COP");

            Money forThree = fare.times(3);

            Assertions.assertThat(forThree).isEqualTo(Money.of("750000.00", "COP"));
        }

        @Test
        @DisplayName("should not treat the same amount in different currencies as equal")
        void shouldNotTreatTheSameAmountInDifferentCurrenciesAsEqual() {
            Money pesos = Money.of("100.00", "COP");
            Money dollars = Money.of("100.00", "USD");

            Assertions.assertThat(pesos).isNotEqualTo(dollars);
        }
    }
}
