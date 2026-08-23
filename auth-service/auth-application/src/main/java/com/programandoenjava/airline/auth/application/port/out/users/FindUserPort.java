package com.programandoenjava.airline.auth.application.port.out.users;

import com.programandoenjava.airline.auth.domain.user.Email;
import com.programandoenjava.airline.auth.domain.user.User;

import java.util.Optional;

public interface FindUserPort {

    Optional<User> byEmail(Email email);
}
