package com.programandoenjava.airline.flight.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        if (currency == null) {
            throw new DomainValidationException("Currency is required");
        }
        if (amount == null) {
            throw new DomainValidationException("Amount is required");
        }
        if (amount.signum() < 0) {
            throw new DomainValidationException("Amount must not be negative, was: " + amount);
        }
        try {
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY);
        } catch (final ArithmeticException exception) {
            throw new DomainValidationException(
                    "Amount " + amount + " has more precision than " + currency + " allows");
        }
    }

    public static Money of(final String amount, final String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public Money plus(final Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(final int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    private void requireSameCurrency(final Money other) {
        if (!currency.equals(other.currency)) {
            throw new DomainValidationException(
                    "Cannot operate on " + currency + " and " + other.currency);
        }
    }
}