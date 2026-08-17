package com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto;

import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Every field is an optional query parameter, so every field is nullable.
 */
public record SearchFlightsRequest(

        @Nullable String origin,

        @Nullable String destination,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Nullable LocalDate date) {
}
