package com.programandoenjava.airline.booking.infrastructure.adapter.out.flight;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class BearerTokenRelay implements RequestInterceptor {

    private static final String BEARER = "Bearer ";
    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final String serviceToken;

    BearerTokenRelay(@Value("${airline.service-token}") final String serviceToken) {
        this.serviceToken = serviceToken;
    }

    /*
     * The user's token first, always. Releasing seats after a refused payment
     * starts from a Kafka message and has no user at all, and that is the only
     * call in this system that falls through to the second branch (ADR-023).
     */
    @Override
    public void apply(final RequestTemplate template) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean carryingAToken = authentication instanceof JwtAuthenticationToken;

        if (carryingAToken) {
            JwtAuthenticationToken caller = (JwtAuthenticationToken) authentication;

            template.header(HttpHeaders.AUTHORIZATION, BEARER + caller.getToken().getTokenValue());
            return;
        }

        template.header(SERVICE_TOKEN_HEADER, serviceToken);
    }
}
