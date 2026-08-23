package com.programandoenjava.airline.auth.application.port.in.authenticate;

public interface AuthenticateUseCase {

    IssuedToken authenticate(AuthenticateCommand command);
}
