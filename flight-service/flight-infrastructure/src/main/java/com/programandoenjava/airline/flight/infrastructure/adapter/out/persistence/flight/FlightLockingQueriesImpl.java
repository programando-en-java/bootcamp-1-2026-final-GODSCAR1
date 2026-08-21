package com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence.flight;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.Optional;
import java.util.UUID;

/**
 * Written with the criteria API and the generated metamodel, the way
 * FlightSpecifications already builds the search.
 *
 * <p>Kept apart from findById on purpose: reading a flight to answer a question
 * must not take a write lock, and one method cannot be both.
 *
 * <p>The entity manager arrives through {@code @PersistenceContext} rather than
 * the constructor. Spring Data builds repository fragments itself, and this is
 * the injection it documents for them.
 */
class FlightLockingQueriesImpl implements FlightLockingQueries {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<FlightEntity> findByIdForUpdate(final UUID id) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<FlightEntity> query = builder.createQuery(FlightEntity.class);
        Root<FlightEntity> flight = query.from(FlightEntity.class);

        query.select(flight)
                .where(builder.equal(flight.get(FlightEntity_.id), id));

        return entityManager.createQuery(query)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }
}
