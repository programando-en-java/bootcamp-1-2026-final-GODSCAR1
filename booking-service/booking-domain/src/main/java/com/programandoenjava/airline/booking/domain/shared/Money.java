package com.programandoenjava.airline.booking.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        if (amount == null) {
            throw new DomainValidationException("An amount is required");
        }
        if (currency == null) {
            throw new DomainValidationException("A currency is required");
        }
        if (amount.signum() < 0) {
            String message = "An amount cannot be negative, was: " + amount;
            throw new DomainValidationException(message);
        }

        int scale = currency.getDefaultFractionDigits();
        try {
            amount = amount.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException carriesTooMuchPrecision) {
            String code = currency.getCurrencyCode();
            String message = "Amount " + amount + " carries more precision than "
                    + code + " allows";
            throw new DomainValidationException(message);
        }
    }

    public static Money of(final String amount, final String currencyCode) {
        BigDecimal parsed = new BigDecimal(amount);
        Currency currency = Currency.getInstance(currencyCode);

        return new Money(parsed, currency);
    }

    public Money times(final int factor) {
        if (factor < 1) {
            String message = "A multiplier must be positive, was: " + factor;
            throw new DomainValidationException(message);
        }

        BigDecimal multiplier = BigDecimal.valueOf(factor);
        BigDecimal multiplied = amount.multiply(multiplier);

        return new Money(multiplied, currency);
    }
}
