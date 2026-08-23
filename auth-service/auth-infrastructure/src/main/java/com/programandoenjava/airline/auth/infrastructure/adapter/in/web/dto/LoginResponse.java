package com.programandoenjava.airline.auth.infrastructure.adapter.in.web.dto;

import com.programandoenjava.airline.auth.application.port.in.authenticate.IssuedToken;

import java.time.Instant;

public record LoginResponse(String accessToken, String tokenType, Instant expiresAt) {

    private static final String BEARER = "Bearer";

    public static LoginResponse from(final IssuedToken token) {
        return new LoginResponse(token.value(), BEARER, token.expiresAt());
    }
}
