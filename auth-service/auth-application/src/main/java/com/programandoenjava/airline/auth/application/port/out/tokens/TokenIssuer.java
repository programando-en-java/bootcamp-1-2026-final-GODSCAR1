package com.programandoenjava.airline.auth.application.port.out.tokens;

import com.programandoenjava.airline.auth.application.port.in.authenticate.IssuedToken;
import com.programandoenjava.airline.auth.domain.user.User;

public interface TokenIssuer {

    IssuedToken issueFor(User user);
}
