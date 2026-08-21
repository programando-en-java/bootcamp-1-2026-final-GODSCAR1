package com.programandoenjava.airline.payment.infrastructure.adapter.out.persistence.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;

class OutboxLockingQueriesImpl implements OutboxLockingQueries {

    private static final String LOCK_TIMEOUT = "jakarta.persistence.lock.timeout";
    private static final int SKIP_LOCKED = -2;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    /* SKIP LOCKED is required, not an optimisation: without it two replicas select
     * the same rows and every event goes out twice. */
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
