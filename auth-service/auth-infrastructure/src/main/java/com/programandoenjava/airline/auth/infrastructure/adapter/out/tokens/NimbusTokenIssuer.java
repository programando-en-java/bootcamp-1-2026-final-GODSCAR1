package com.programandoenjava.airline.auth.infrastructure.adapter.out.tokens;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.programandoenjava.airline.auth.application.port.in.authenticate.IssuedToken;
import com.programandoenjava.airline.auth.application.port.out.tokens.TokenIssuer;
import com.programandoenjava.airline.auth.domain.user.Role;
import com.programandoenjava.airline.auth.domain.user.User;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

class NimbusTokenIssuer implements TokenIssuer {

    static final String ROLES_CLAIM = "roles";

    private final RSAKey signingKey;
    private final String issuer;
    private final Duration lifetime;
    private final Clock clock;

    NimbusTokenIssuer(final RSAKey signingKey,
                      final String issuer,
                      final Duration lifetime,
                      final Clock clock) {
        this.signingKey = signingKey;
        this.issuer = issuer;
        this.lifetime = lifetime;
        this.clock = clock;
    }

    @Override
    public IssuedToken issueFor(final User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(lifetime);

        List<String> roles = user.roles().stream().map(Role::name).sorted().toList();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.id().value().toString())
                .issuer(issuer)
                .claim(ROLES_CLAIM, roles)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .build();

        String token = sign(claims);

        return new IssuedToken(token, expiresAt);
    }

    private String sign(final JWTClaimsSet claims) {
        /* typ is not optional: Spring Security 7 validates it by default and
         * rejects a token without one, with a message that says nothing useful. */
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(signingKey.getKeyID())
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);

        try {
            jwt.sign(new RSASSASigner(signingKey));
        } catch (JOSEException failed) {
            throw new IllegalStateException("The token could not be signed", failed);
        }

        return jwt.serialize();
    }
}
