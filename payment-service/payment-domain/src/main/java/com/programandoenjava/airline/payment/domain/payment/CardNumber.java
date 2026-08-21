package com.programandoenjava.airline.payment.domain.payment;

import com.programandoenjava.airline.payment.domain.shared.DomainValidationException;

import java.util.regex.Pattern;

public record CardNumber(String value) {

    private static final Pattern DIGITS = Pattern.compile("^\\d{13,19}$");
    private static final int VISIBLE_DIGITS = 4;

    public CardNumber {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("A card number is required");
        }
        value = value.replace(" ", "").replace("-", "");
        if (!DIGITS.matcher(value).matches()) {
            throw new DomainValidationException("A card number is 13 to 19 digits");
        }
        if (!passesLuhn(value)) {
            throw new DomainValidationException("That card number is not valid");
        }
    }

    public String lastFourDigits() {
        int from = value.length() - VISIBLE_DIGITS;

        return value.substring(from);
    }

    @Override
    public String toString() {
        return "****" + lastFourDigits();
    }

    private static boolean passesLuhn(final String digits) {
        int sum = 0;
        boolean doubling = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubling) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = digit - 9;
                }
            }
            sum = sum + digit;
            doubling = !doubling;
        }

        return sum % 10 == 0;
    }
}
