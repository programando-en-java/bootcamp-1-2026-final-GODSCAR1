package com.programandoenjava.airline.auth.domain.user;

import com.programandoenjava.airline.auth.domain.shared.DomainValidationException;

public record PasswordHash(String value) {

    private static final String BCRYPT_PREFIX = "$2";

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("A password hash is required");
        }

        boolean looksHashed = value.startsWith(BCRYPT_PREFIX);

        /* The one check worth making here: a plain password reaching this type
         * is the mistake that ends up in a database, and it is silent. */
        if (!looksHashed) {
            throw new DomainValidationException("A password must be hashed before it is stored");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[hidden]";
    }
}
