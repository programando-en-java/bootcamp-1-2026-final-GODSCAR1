package com.programandoenjava.airline.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String HEALTH = "/actuator/health/**";
    private static final String LOGIN = "/api/v1/auth/**";
    private static final String ERROR = "/error";
    private static final String SEARCH = "/api/v1/flights";
    private static final String ONE_FLIGHT = "/api/v1/flights/*";
    private static final String SEAT_BLOCKS = "/api/v1/flights/*/seat-blocks/**";

    @Bean
    SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HEALTH).permitAll()

                        /*
                         * Tomcat forwards a failed request here, and this is a
                         * separate request as far as security is concerned. Left
                         * out, every downstream failure comes back as a 401 from
                         * the error page and hides what actually went wrong.
                         */
                        .requestMatchers(ERROR).permitAll()

                        .requestMatchers(LOGIN).permitAll()

                        /* Nobody logs in to look at what is on sale. */
                        .requestMatchers(HttpMethod.GET, SEARCH, ONE_FLIGHT).permitAll()

                        /*
                         * What this gateway is actually for. Holding and
                         * releasing seats is booking-service's business and no
                         * caller's, so it is not reachable from outside at all.
                         * flight-service still checks the service token: this is
                         * the second lock, not the only one (ADR-024).
                         */
                        .requestMatchers(SEAT_BLOCKS).denyAll()

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
