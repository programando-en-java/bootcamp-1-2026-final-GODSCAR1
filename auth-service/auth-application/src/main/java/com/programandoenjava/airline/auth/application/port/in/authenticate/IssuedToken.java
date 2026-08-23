package com.programandoenjava.airline.auth.application.port.in.authenticate;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt) {
}
