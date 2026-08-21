package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;

/**
 * Written with the criteria API and the generated metamodel, so a renamed field
 * is a compilation error rather than a query that fails on the first sweep.
 *
 * <p>The lock mode and the hint are set on the query rather than declared with
 * {@code @Lock}: -2 is Hibernate's spelling of SKIP LOCKED, and there is
 * nowhere else to put it once the string is gone.
 *
 * <p>The entity manager arrives through {@code @PersistenceContext} rather
 * than the constructor, which is the one place in this codebase that does so.
 * Spring Data builds repository fragments itself, and this is the injection it
 * documents for them.
 */
class OutboxLockingQueriesImpl implements OutboxLockingQueries {

    private static final String LOCK_TIMEOUT = "jakarta.persistence.lock.timeout";
    private static final int SKIP_LOCKED = -2;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<OutboxEntity> claimPending(final int limit) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<OutboxEntity> query = builder.createQuery(OutboxEntity.class);
        Root<OutboxEntity> outbox = query.from(OutboxEntity.class);

        query.select(outbox)
                .where(builder.isNull(outbox.get(OutboxEntity_.publishedAt)))
                .orderBy(builder.asc(outbox.get(OutboxEntity_.createdAt)));

        return entityManager.createQuery(query)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(LOCK_TIMEOUT, SKIP_LOCKED)
                .setMaxResults(limit)
                .getResultList();
    }
}
