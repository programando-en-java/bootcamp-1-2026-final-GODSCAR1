package com.programandoenjava.airline.checkin.application.usecase;

import com.programandoenjava.airline.checkin.application.event.CheckInCompleted;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.NextBoardingSequencePort;
import com.programandoenjava.airline.checkin.application.port.out.boardingpass.SaveBoardingPassPort;
import com.programandoenjava.airline.checkin.application.port.out.events.DomainEventPublisher;
import com.programandoenjava.airline.checkin.application.transaction.Isolation;
import com.programandoenjava.airline.checkin.application.transaction.UnitOfWork;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingPass;
import com.programandoenjava.airline.checkin.domain.boardingpass.BoardingSequence;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.domain.boardingpass.FlightSnapshot;
import com.programandoenjava.airline.checkin.domain.boardingpass.PassengerId;

import java.time.Instant;
import java.util.Optional;

/**
 * The writes that belong together: taking a place in the boarding order,
 * printing the pass that carries it, and the outbox row the listener adds when
 * the event is published (ADR-001).
 *
 * <p>A class of its own rather than a method on CheckInService, because
 * {@code @UnitOfWork} is proxy-based and does nothing when a bean calls itself.
 * Keeping it here also keeps the transaction off the two network calls that
 * precede it.
 */
public class BoardingPassIssuer {

    private final NextBoardingSequencePort nextBoardingSequence;
    private final SaveBoardingPassPort saveBoardingPass;
    private final DomainEventPublisher events;

    public BoardingPassIssuer(final NextBoardingSequencePort nextBoardingSequence,
                              final SaveBoardingPassPort saveBoardingPass,
                              final DomainEventPublisher events) {
        this.nextBoardingSequence = nextBoardingSequence;
        this.saveBoardingPass = saveBoardingPass;
        this.events = events;
    }

    /*
     * A number can be skipped: if two requests for the same booking arrive
     * together, both take one and only one pass is written. Nothing boards by a
     * number that has to be contiguous, and paying for that with a lock held
     * across every check-in on the flight would be the worse trade.
     */
    @UnitOfWork(isolation = Isolation.SERIALIZABLE)
    public BoardingPass issue(final BookingId bookingId,
                              final PassengerId passengerId,
                              final FlightSnapshot flight,
                              final Instant now) {
        BoardingSequence sequence = nextBoardingSequence.nextFor(flight.flightId());

        BoardingPass pass = BoardingPass.issue(bookingId, passengerId, flight, sequence, now);

        Optional<BoardingPass> alreadyIssued = saveBoardingPass.saveIfNew(pass);
        if (alreadyIssued.isPresent()) {
            return alreadyIssued.get();
        }

        events.publish(new CheckInCompleted(pass));

        return pass;
    }
}
