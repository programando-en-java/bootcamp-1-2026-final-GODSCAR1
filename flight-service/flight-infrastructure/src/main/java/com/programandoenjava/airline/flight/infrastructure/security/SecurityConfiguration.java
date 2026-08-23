package com.programandoenjava.airline.flight.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ServiceTokens.class)
public class SecurityConfiguration {

    private static final String HEALTH = "/actuator/health/**";

    private static final String SEARCH = "/api/v1/flights";
    private static final String ONE_FLIGHT = "/api/v1/flights/*";

    @Bean
    SecurityFilterChain filterChain(final HttpSecurity http,
                                    final ServiceTokens serviceTokens) throws Exception {
        return http
                /* Before the bearer filter, though the order does not matter:
                 * a service call carries no Authorization and a user's carries
                 * no X-Service-Token, so the two never meet. */
                .addFilterBefore(new ServiceTokenAuthenticationFilter(serviceTokens),
                        UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HEALTH).permitAll()
                        .requestMatchers(HttpMethod.GET, SEARCH, ONE_FLIGHT).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
