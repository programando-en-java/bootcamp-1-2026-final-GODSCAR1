package com.programandoenjava.airline.checkin.infrastructure.adapter.in.web;

import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInCommand;
import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInUseCase;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.infrastructure.adapter.in.web.dto.BoardingPassResponse;
import com.programandoenjava.airline.checkin.infrastructure.adapter.in.web.dto.CheckInRequest;
import com.programandoenjava.airline.checkin.application.port.shared.Caller;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/boarding-passes")
public class CheckInController {

    private final CheckInUseCase checkInUseCase;

    CheckInController(final CheckInUseCase checkInUseCase) {
        this.checkInUseCase = checkInUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BoardingPassResponse checkIn(@AuthenticationPrincipal final Jwt token,
                                 @Valid @RequestBody final CheckInRequest request) {
        Caller caller = CallerFromToken.of(token);

        CheckInCommand command = CheckInRequestMapper.toCommand(request, caller);

        BoardingPass pass = checkInUseCase.checkIn(command);

        return BoardingPassResponse.from(pass);
    }
}
