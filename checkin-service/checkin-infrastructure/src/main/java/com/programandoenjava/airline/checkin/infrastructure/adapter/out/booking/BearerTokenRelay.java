package com.programandoenjava.airline.checkin.infrastructure.adapter.out.booking;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class BearerTokenRelay implements RequestInterceptor {

    private static final String BEARER = "Bearer ";

    @Override
    public void apply(final RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean carryingAToken = authentication instanceof JwtAuthenticationToken;

        if (!carryingAToken) {
            return;
        }

        JwtAuthenticationToken caller = (JwtAuthenticationToken) authentication;

        template.header(HttpHeaders.AUTHORIZATION, BEARER + caller.getToken().getTokenValue());
    }
}
