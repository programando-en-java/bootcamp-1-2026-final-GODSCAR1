package com.programandoenjava.airline.auth.infrastructure.adapter.out.passwords;

import com.programandoenjava.airline.auth.application.port.out.passwords.PasswordVerifier;
import com.programandoenjava.airline.auth.domain.user.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptPasswordVerifier implements PasswordVerifier {

    private static final String NOBODY = "$2a$10$7EqJtq98hPqEX7fNZaFWoOa1u8FrH1LPBRVjLDCgTBTHNhZ7Tn7Zu";

    private final BCryptPasswordEncoder encoder;

    BCryptPasswordVerifier(final BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public boolean matches(final String password, final PasswordHash hash) {
        return encoder.matches(password, hash.value());
    }

    @Override
    public void wasteTime() {
        encoder.matches("", NOBODY);
    }
}
