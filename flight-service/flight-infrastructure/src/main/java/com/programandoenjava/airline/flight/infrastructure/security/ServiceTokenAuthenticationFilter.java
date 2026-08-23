package com.programandoenjava.airline.flight.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Service-Token";
    static final String SERVICE_ROLE = "ROLE_SERVICE";

    /*
     * Only seat blocks. A secret that opens everything is how one that leaks
     * becomes full access, and nothing else here is called service to service.
     */
    private static final String SCOPE = "/seat-blocks";

    private final Map<String, String> tokens;

    ServiceTokenAuthenticationFilter(final ServiceTokens serviceTokens) {
        this.tokens = serviceTokens.serviceTokens();
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain chain) throws ServletException, IOException {

        String presented = request.getHeader(HEADER);
        boolean withinScope = request.getRequestURI().contains(SCOPE);

        if (presented != null && withinScope) {
            whoPresented(presented).ifPresent(ServiceTokenAuthenticationFilter::authenticate);
        }

        /* Never rejects. A request without this header is a user's, and the
         * bearer filter behind this one is what decides about those. */
        chain.doFilter(request, response);
    }

    /* Compared byte by byte in constant time. equals stops at the first
     * difference, and how long it took says how much of the secret was right. */
    private Optional<String> whoPresented(final String presented) {
        byte[] offered = presented.getBytes(StandardCharsets.UTF_8);

        return tokens.entrySet().stream()
                .filter(known -> MessageDigest.isEqual(
                        offered, known.getValue().getBytes(StandardCharsets.UTF_8)))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static void authenticate(final String service) {
        UsernamePasswordAuthenticationToken caller = new UsernamePasswordAuthenticationToken(
                service, null, List.of(new SimpleGrantedAuthority(SERVICE_ROLE)));

        SecurityContextHolder.getContext().setAuthentication(caller);
    }
}
