package com.programandoenjava.airline.auth.infrastructure.config;

import com.programandoenjava.airline.auth.application.port.in.authenticate.AuthenticateUseCase;
import com.programandoenjava.airline.auth.application.port.out.passwords.PasswordVerifier;
import com.programandoenjava.airline.auth.application.port.out.tokens.TokenIssuer;
import com.programandoenjava.airline.auth.application.port.out.users.FindUserPort;
import com.programandoenjava.airline.auth.application.usecase.AuthenticateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuthenticateUseCase authenticateUseCase(final FindUserPort findUserPort,
                                            final PasswordVerifier passwordVerifier,
                                            final TokenIssuer tokenIssuer) {
        return new AuthenticateService(findUserPort, passwordVerifier, tokenIssuer);
    }
}
