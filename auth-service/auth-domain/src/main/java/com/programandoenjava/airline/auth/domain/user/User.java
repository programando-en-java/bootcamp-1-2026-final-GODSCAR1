package com.programandoenjava.airline.auth.domain.user;

import com.programandoenjava.airline.auth.domain.shared.DomainValidationException;

import java.time.Instant;
import java.util.Set;

public record User(UserId id,
                   Email email,
                   PasswordHash passwordHash,
                   Set<Role> roles,
                   Instant createdAt) {

    public User {
        if (id == null) {
            throw new DomainValidationException("A user id is required");
        }
        if (email == null) {
            throw new DomainValidationException("A user must have an email");
        }
        if (passwordHash == null) {
            throw new DomainValidationException("A user must have a password");
        }
        if (roles == null || roles.isEmpty()) {
            throw new DomainValidationException("A user must have at least one role");
        }
        if (createdAt == null) {
            throw new DomainValidationException("A user must record when it was created");
        }

        roles = Set.copyOf(roles);
    }

    public static User register(final Email email,
                                final PasswordHash passwordHash,
                                final Set<Role> roles,
                                final Instant now) {
        return new User(UserId.newId(), email, passwordHash, roles, now);
    }

    public boolean hasRole(final Role role) {
        return roles.contains(role);
    }
}
