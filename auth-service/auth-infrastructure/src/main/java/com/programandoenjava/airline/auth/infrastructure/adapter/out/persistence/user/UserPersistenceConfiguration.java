package com.programandoenjava.airline.auth.infrastructure.adapter.out.persistence.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserPersistenceConfiguration {

    @Bean
    UserPersistenceAdapter userPersistenceAdapter(final UserJpaRepository userJpaRepository) {
        return new UserPersistenceAdapter(userJpaRepository);
    }
}
