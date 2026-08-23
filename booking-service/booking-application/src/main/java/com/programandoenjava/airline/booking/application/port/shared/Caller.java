package com.programandoenjava.airline.booking.application.port.shared;

import com.programandoenjava.airline.booking.domain.shared.DomainValidationException;

import java.util.Set;
import java.util.UUID;

public record Caller(UUID id, Set<Role> roles) {

    public Caller {
        if (id == null) {
            throw new DomainValidationException("A caller must be identified");
        }
        if (roles == null) {
            throw new DomainValidationException("A caller's roles are required");
        }

        roles = Set.copyOf(roles);
    }

    public boolean is(final UUID somebody) {
        return id.equals(somebody);
    }

    public boolean isStaff() {
        return roles.contains(Role.AGENT) || roles.contains(Role.ADMIN);
    }
}
