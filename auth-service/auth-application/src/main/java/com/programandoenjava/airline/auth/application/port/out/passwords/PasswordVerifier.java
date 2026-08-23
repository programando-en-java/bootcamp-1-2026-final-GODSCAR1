package com.programandoenjava.airline.auth.application.port.out.passwords;

import com.programandoenjava.airline.auth.domain.user.PasswordHash;

public interface PasswordVerifier {

    boolean matches(String password, PasswordHash hash);

    /* Run when no user was found, so that answering an unknown email takes as
     * long as answering a wrong password. */
    void wasteTime();
}
