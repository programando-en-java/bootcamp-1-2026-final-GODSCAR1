package com.programandoenjava.airline.auth.application.port.in.authenticate.exception;

/* One exception for both an unknown email and a wrong password. Telling them
 * apart tells an attacker which emails are registered. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Those credentials do not match an account");
    }
}
