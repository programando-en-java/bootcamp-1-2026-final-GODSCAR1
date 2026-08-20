package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * One row per flight, holding the last place handed out. The row is what two
 * passengers checking in at the same moment take turns over: it is read for
 * update, so the second waits rather than being given the number the first has.
 */
@Entity
@Table(name = "boarding_sequences")
class BoardingSequenceEntity {

    @Id
    @Column(name = "flight_id")
    private UUID flightId;

    @Column(name = "last_sequence", nullable = false)
    private int lastSequence;

    protected BoardingSequenceEntity() {
        // required by JPA
    }

    int takeNext() {
        lastSequence = lastSequence + 1;

        return lastSequence;
    }
}
