package com.programandoenjava.airline.auth.infrastructure.adapter.out.persistence.user;

import com.programandoenjava.airline.auth.domain.user.Email;
import com.programandoenjava.airline.auth.domain.user.PasswordHash;
import com.programandoenjava.airline.auth.domain.user.Role;
import com.programandoenjava.airline.auth.domain.user.User;
import com.programandoenjava.airline.auth.domain.user.UserId;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

final class UserEntityMapper {

    private static final String SEPARATOR = ",";

    private UserEntityMapper() {
    }

    static User toDomain(final UserEntity entity) {
        Set<Role> roles = Arrays.stream(entity.getRoles().split(SEPARATOR))
                .map(String::trim)
                .map(Role::valueOf)
                .collect(Collectors.toUnmodifiableSet());

        return new User(
                new UserId(entity.getId()),
                new Email(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                roles,
                entity.getCreatedAt());
    }
}
