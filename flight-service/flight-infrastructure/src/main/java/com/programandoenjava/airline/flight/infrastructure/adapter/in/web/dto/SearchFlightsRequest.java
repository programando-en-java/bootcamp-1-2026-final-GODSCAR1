package com.programandoenjava.airline.flight.infrastructure.adapter.in.web.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record SearchFlightsRequest(

        String origin,

        String destination,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date) {
}