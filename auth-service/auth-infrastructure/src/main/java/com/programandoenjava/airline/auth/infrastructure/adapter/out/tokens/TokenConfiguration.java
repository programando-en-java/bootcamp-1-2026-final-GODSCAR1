package com.programandoenjava.airline.auth.infrastructure.adapter.out.tokens;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.programandoenjava.airline.auth.application.port.out.tokens.TokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Configuration
public class TokenConfiguration {

    private static final int KEY_SIZE = 2048;

    /**
     * Generated at startup rather than read from a file, so that no private key
     * is ever committed. The cost is that a restart invalidates every token
     * already handed out and everyone logs in again (ADR-020).
     */
    @Bean
    RSAKey signingKey() {
        try {
            return new RSAKeyGenerator(KEY_SIZE)
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
        } catch (JOSEException failed) {
            throw new IllegalStateException("No signing key could be generated", failed);
        }
    }

    @Bean
    TokenIssuer tokenIssuer(final RSAKey signingKey,
                            @Value("${airline.auth.issuer}") final String issuer,
                            @Value("${airline.auth.token-lifetime}") final Duration lifetime,
                            final Clock clock) {
        return new NimbusTokenIssuer(signingKey, issuer, lifetime, clock);
    }
}
