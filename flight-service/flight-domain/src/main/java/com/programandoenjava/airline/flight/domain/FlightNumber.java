package com.programandoenjava.airline.flight.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record FlightNumber(String value) {

    private static final Pattern DESIGNATOR = Pattern.compile("^[A-Za-z0-9]{2}\\d{1,4}$");

    public FlightNumber {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Flight number is required");
        }
        String normalised = value.trim();
        if (!DESIGNATOR.matcher(normalised).matches()) {
            throw new DomainValidationException(
                    "Flight number must be an IATA designator, was: " + value);
        }
        value = normalised.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
