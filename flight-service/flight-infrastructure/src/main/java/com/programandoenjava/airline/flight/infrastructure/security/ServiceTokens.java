package com.programandoenjava.airline.flight.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "airline")
public record ServiceTokens(Map<String, String> serviceTokens) {

    public ServiceTokens {
        serviceTokens = serviceTokens == null ? Map.of() : Map.copyOf(serviceTokens);
    }
}
