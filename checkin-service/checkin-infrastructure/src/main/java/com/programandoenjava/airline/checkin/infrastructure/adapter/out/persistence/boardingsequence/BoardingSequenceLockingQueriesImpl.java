package com.programandoenjava.airline.checkin.infrastructure.adapter.out.persistence.boardingsequence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.Optional;
import java.util.UUID;

class BoardingSequenceLockingQueriesImpl implements BoardingSequenceLockingQueries {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<BoardingSequenceEntity> findByFlightForUpdate(final UUID flightId) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<BoardingSequenceEntity> query =
                builder.createQuery(BoardingSequenceEntity.class);
        Root<BoardingSequenceEntity> counter = query.from(BoardingSequenceEntity.class);

        query.select(counter)
                .where(builder.equal(counter.get(BoardingSequenceEntity_.flightId), flightId));

        return entityManager.createQuery(query)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }
}
