package com.programandoenjava.airline.booking.application.port.shared;

public record IdempotencyKey(String value) {

    public static final int MAX_LENGTH = 255;

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("An idempotency key is required");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            String message = "An idempotency key cannot exceed "
                    + MAX_LENGTH + " characters";
            throw new IllegalArgumentException(message);
        }
    }
}
