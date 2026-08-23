package com.programandoenjava.airline.checkin.infrastructure.adapter.in.web;

import com.programandoenjava.airline.checkin.application.port.shared.Caller;
import com.programandoenjava.airline.checkin.application.port.shared.Role;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class CallerFromToken {

    private static final String ROLES_CLAIM = "roles";

    private CallerFromToken() {
    }

    static Caller of(final Jwt token) {
        UUID id = UUID.fromString(token.getSubject());

        List<String> names = token.getClaimAsStringList(ROLES_CLAIM);

        if (names == null) {
            return new Caller(id, Set.of());
        }

        Set<Role> roles = names.stream()
                .map(CallerFromToken::known)
                .filter(role -> role != null)
                .collect(Collectors.toUnmodifiableSet());

        return new Caller(id, roles);
    }

    private static Role known(final String name) {
        try {
            return Role.valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
