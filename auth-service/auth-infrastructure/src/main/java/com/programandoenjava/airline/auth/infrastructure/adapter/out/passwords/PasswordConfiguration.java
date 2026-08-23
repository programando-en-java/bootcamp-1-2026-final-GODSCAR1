package com.programandoenjava.airline.auth.infrastructure.adapter.out.passwords;

import com.programandoenjava.airline.auth.application.port.out.passwords.PasswordVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PasswordConfiguration {

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    PasswordVerifier passwordVerifier(final BCryptPasswordEncoder passwordEncoder) {
        return new BCryptPasswordVerifier(passwordEncoder);
    }
}
