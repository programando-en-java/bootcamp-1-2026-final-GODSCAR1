package com.programandoenjava.airline.auth.infrastructure.adapter.in.web;

import com.programandoenjava.airline.auth.application.port.in.authenticate.AuthenticateCommand;
import com.programandoenjava.airline.auth.application.port.in.authenticate.AuthenticateUseCase;
import com.programandoenjava.airline.auth.application.port.in.authenticate.IssuedToken;
import com.programandoenjava.airline.auth.infrastructure.adapter.in.web.dto.LoginRequest;
import com.programandoenjava.airline.auth.infrastructure.adapter.in.web.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUseCase authenticateUseCase;

    AuthController(final AuthenticateUseCase authenticateUseCase) {
        this.authenticateUseCase = authenticateUseCase;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody final LoginRequest request) {
        AuthenticateCommand command = new AuthenticateCommand(request.email(), request.password());

        IssuedToken token = authenticateUseCase.authenticate(command);

        return LoginResponse.from(token);
    }
}
