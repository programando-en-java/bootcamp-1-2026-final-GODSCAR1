package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "boarding_sequences")
class BoardingSequenceEntity {

    @Id
    @Column(name = "flight_id")
    private UUID flightId;

    @Column(name = "last_sequence", nullable = false)
    private int lastSequence;

    protected BoardingSequenceEntity() {
    }

    BoardingSequenceEntity(final UUID flightId) {
        this.flightId = flightId;
        this.lastSequence = 0;
    }

    int takeNext() {
        lastSequence = lastSequence + 1;

        return lastSequence;
    }
}
