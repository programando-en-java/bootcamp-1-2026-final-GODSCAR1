package com.programandoenjava.airline.checkin.application.event;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;

public record CheckInCompleted(BoardingPass boardingPass) {

    public CheckInCompleted {
        if (boardingPass == null) {
            throw new IllegalArgumentException("A boarding pass is required");
        }
    }
}
