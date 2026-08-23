package com.programandoenjava.airline.auth.application.usecase;

import com.programandoenjava.airline.auth.application.port.in.authenticate.AuthenticateCommand;
import com.programandoenjava.airline.auth.application.port.in.authenticate.AuthenticateUseCase;
import com.programandoenjava.airline.auth.application.port.in.authenticate.IssuedToken;
import com.programandoenjava.airline.auth.application.port.in.authenticate.exception.InvalidCredentialsException;
import com.programandoenjava.airline.auth.application.port.out.passwords.PasswordVerifier;
import com.programandoenjava.airline.auth.application.port.out.tokens.TokenIssuer;
import com.programandoenjava.airline.auth.application.port.out.users.FindUserPort;
import com.programandoenjava.airline.auth.domain.user.Email;
import com.programandoenjava.airline.auth.domain.user.User;

import java.util.Optional;

public class AuthenticateService implements AuthenticateUseCase {

    private final FindUserPort findUser;
    private final PasswordVerifier passwordVerifier;
    private final TokenIssuer tokenIssuer;

    public AuthenticateService(final FindUserPort findUser,
                               final PasswordVerifier passwordVerifier,
                               final TokenIssuer tokenIssuer) {
        this.findUser = findUser;
        this.passwordVerifier = passwordVerifier;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public IssuedToken authenticate(final AuthenticateCommand command) {
        Email email = new Email(command.email());

        Optional<User> found = findUser.byEmail(email);

        if (found.isEmpty()) {
            passwordVerifier.wasteTime();
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        boolean correct = passwordVerifier.matches(command.password(), user.passwordHash());

        if (!correct) {
            throw new InvalidCredentialsException();
        }

        return tokenIssuer.issueFor(user);
    }
}
