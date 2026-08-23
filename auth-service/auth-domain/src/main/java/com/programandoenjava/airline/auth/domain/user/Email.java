package com.programandoenjava.airline.auth.domain.user;

import com.programandoenjava.airline.auth.domain.shared.DomainValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static final int MAX_LENGTH = 254;

    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("An email is required");
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException(
                    "An email cannot be longer than " + MAX_LENGTH + " characters");
        }

        boolean wellFormed = SHAPE.matcher(value).matches();

        if (!wellFormed) {
            throw new DomainValidationException("Not an email address: " + value);
        }
    }
}
