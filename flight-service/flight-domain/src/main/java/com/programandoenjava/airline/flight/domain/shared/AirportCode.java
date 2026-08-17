package com.programandoenjava.airline.flight.domain.shared;

import java.util.Locale;
import java.util.regex.Pattern;

public record AirportCode(String value) {

    private static final Pattern IATA = Pattern.compile("^[A-Za-z]{3}$");

    public AirportCode {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Airport code is required");
        }
        String normalised = value.trim();
        if (!IATA.matcher(normalised).matches()) {
            throw new DomainValidationException(
                    "Airport code must be three letters, was: " + value);
        }
        value = normalised.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
