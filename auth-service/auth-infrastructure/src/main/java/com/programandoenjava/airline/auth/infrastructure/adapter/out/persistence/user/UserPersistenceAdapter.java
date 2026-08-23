package com.programandoenjava.airline.auth.infrastructure.adapter.out.persistence.user;

import com.programandoenjava.airline.auth.application.port.out.users.FindUserPort;
import com.programandoenjava.airline.auth.domain.user.Email;
import com.programandoenjava.airline.auth.domain.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

class UserPersistenceAdapter implements FindUserPort {

    private final UserJpaRepository userJpaRepository;

    UserPersistenceAdapter(final UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> byEmail(final Email email) {
        return userJpaRepository.findByEmail(email.value())
                .map(UserEntityMapper::toDomain);
    }
}
