package com.programandoenjava.airline.auth.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "An email is required")
        String email,

        @NotBlank(message = "A password is required")
        String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=hidden]";
    }
}
