package com.programandoenjava.airline.checkin.infrastructure.adapter.out.events;

/**
 * One topic, because check-in has one outcome worth announcing. A refusal is
 * answered to the passenger and nothing downstream acts on it.
 */
public final class CheckInTopics {

    public static final String COMPLETED = "checkin.completed.v1";

    private CheckInTopics() {
    }
}
