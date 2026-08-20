package com.programandoenjava.airline.checkin.domain.checkin;

import com.programandoenjava.airline.checkin.domain.checkin.exception.CheckInClosedException;
import com.programandoenjava.airline.checkin.domain.checkin.exception.CheckInNotOpenYetException;
import com.programandoenjava.airline.checkin.domain.checkin.exception.FlightDepartedException;
import com.programandoenjava.airline.checkin.domain.shared.DomainValidationException;

import java.time.Duration;
import java.time.Instant;

/**
 * When check-in is allowed, expressed as two distances from departure. The
 * durations come from configuration, so the rule is here and the numbers are
 * not.
 *
 * <p>Three refusals rather than one, because they call for different things
 * from the passenger: come back later, go to the counter, and the flight has
 * gone.
 */
public record CheckInWindow(Duration opensBefore, Duration closesBefore) {

    public CheckInWindow {
        if (opensBefore == null || closesBefore == null) {
            throw new DomainValidationException("A check-in window needs both of its bounds");
        }
        if (closesBefore.isNegative()) {
            throw new DomainValidationException(
                    "A check-in window cannot close after departure, was: " + closesBefore);
        }
        if (!opensBefore.minus(closesBefore).isPositive()) {
            throw new DomainValidationException(
                    "A check-in window must open before it closes, was: "
                            + opensBefore + " to " + closesBefore);
        }
    }

    public Instant opensFor(final Instant departure) {
        return departure.minus(opensBefore);
    }

    public Instant closesFor(final Instant departure) {
        return departure.minus(closesBefore);
    }

    /**
     * Departure is tested first. A flight that has left is also past its
     * closing time, and the vaguer of the two answers is the less useful one.
     */
    public void requireOpenAt(final Instant departure, final Instant now) {
        if (!now.isBefore(departure)) {
            throw new FlightDepartedException(
                    "The flight departed at " + departure + " and check-in is over");
        }

        Instant opens = opensFor(departure);
        if (now.isBefore(opens)) {
            throw new CheckInNotOpenYetException("Check-in for this flight opens at " + opens);
        }

        Instant closes = closesFor(departure);
        if (!now.isBefore(closes)) {
            throw new CheckInClosedException("Check-in for this flight closed at " + closes);
        }
    }
}
