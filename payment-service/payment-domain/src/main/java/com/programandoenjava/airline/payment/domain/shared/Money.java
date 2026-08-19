package com.programandoenjava.airline.payment.domain.shared;

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
        if (amount.signum() <= 0) {
            String message = "An amount must be positive, was: " + amount;
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
}
