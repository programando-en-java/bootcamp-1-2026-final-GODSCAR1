package com.programandoenjava.airline.auth.application.port.in.authenticate;

import com.programandoenjava.airline.auth.domain.shared.DomainValidationException;

public record AuthenticateCommand(String email, String password) {

    public AuthenticateCommand {
        if (email == null || email.isBlank()) {
            throw new DomainValidationException("An email is required");
        }
        if (password == null || password.isBlank()) {
            throw new DomainValidationException("A password is required");
        }
    }

    @Override
    public String toString() {
        return "AuthenticateCommand[email=" + email + ", password=hidden]";
    }
}
