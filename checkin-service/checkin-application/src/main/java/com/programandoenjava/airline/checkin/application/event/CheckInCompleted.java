package com.programandoenjava.airline.checkin.application.event;

import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;

/**
 * A passenger checked in and a pass was printed. In process only: it carries
 * value objects and is never serialised. What crosses the wire is the flat
 * integration event the listener maps this into (ADR-001).
 */
public record CheckInCompleted(BoardingPass boardingPass) {

    public CheckInCompleted {
        if (boardingPass == null) {
            throw new IllegalArgumentException("A boarding pass is required");
        }
    }
}
