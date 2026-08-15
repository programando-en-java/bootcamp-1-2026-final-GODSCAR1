# Architecture

Architecture Decision Records for the airline reservation system.

Each record states the context, the decision, and the consequences we accept.
Records marked **Open** are not yet decided and are pending discussion.

| ADR | Status | Decision |
|-----|--------|----------|
| [001](#adr-001-transactional-outbox-for-integration-events) | Accepted | Transactional outbox for integration events |
| [002](#adr-002-postgresql-in-every-environment) | Accepted | PostgreSQL in every environment, no H2 |
| [003](#adr-003-no-shared-module) | Accepted | No shared module; deliberate duplication |
| [004](#adr-004-transport-for-saga-commands) | Open | Transport for saga commands |
| [005](#adr-005-jwt-issuer) | Open | JWT issuer |
| [006](#adr-006-hexagonal-architecture-with-one-maven-module-per-layer) | **Proposed** | Hexagonal architecture, one Maven module per layer |

---

## ADR-001: Transactional outbox for integration events

**Status:** Accepted

### Context

The project brief describes an event-driven architecture using "Spring Events"
with `BookingCreatedEvent`, `PaymentProcessedEvent` and `CheckInCompletedEvent`.
Spring's `ApplicationEventPublisher` is in-process only and cannot cross service
boundaries, so it cannot be the transport between microservices.

We separate two distinct concepts:

- **Domain event** — an in-process record published via `ApplicationEventPublisher`.
  Internal to the service, free to reference value objects, never serialized.
- **Integration event** — a flat DTO published to Kafka. This is the public
  contract between services and is versioned.

Publishing to Kafka directly from a `@TransactionalEventListener(AFTER_COMMIT)`
creates a dual write: if the broker is unreachable after the database commits,
the event is lost with no trace. If instead the send happens before the commit
and the transaction later rolls back, we have announced something that never
happened.

### Decision

Persist integration events to an `outbox` table inside the same transaction as
the state change, and publish them asynchronously with a relay.

The listener runs at `BEFORE_COMMIT`, so the insert joins the business
transaction:

```java
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
void on(BookingCreated event) {
    BookingCreatedIntegrationEvent payload = mapper.toIntegration(event);
    outboxRepository.save(OutboxRecord.of("booking", event.bookingId(), "booking.created.v1", payload));
}
```

Table shape (per service):

```sql
CREATE TABLE outbox (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(64)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pending ON outbox (created_at) WHERE published_at IS NULL;
```

The partial index stays small as the table grows.

The relay is a scheduled poller:

```sql
SELECT * FROM outbox
WHERE published_at IS NULL
ORDER BY created_at
LIMIT :batchSize
FOR UPDATE SKIP LOCKED
```

`SKIP LOCKED` is required, not an optimization: without it two replicas of the
same service select the same rows and publish every event twice.

The relay is deliberately generic. It knows nothing about bookings or payments;
it reads rows and sends `payload` to `topic` keyed by `aggregate_id`. Keying by
aggregate id routes all events for one aggregate to the same partition, which
preserves per-aggregate ordering.

Services owning an outbox: `booking`, `payment`, `checkin`. `flight` only
responds to commands and publishes nothing. `auth` and `notification` publish
nothing.

### Consequences

- **Delivery is at-least-once.** If Kafka acknowledges the send and the
  subsequent `UPDATE published_at` fails, the event is republished on the next
  poll. **Every consumer must be idempotent**, deduplicating on the outbox `id`,
  which travels with the payload. This is a hard requirement, not a
  recommendation.
- Events are delivered with a delay bounded by the polling interval.
- The table grows without limit unless published rows are purged. A retention
  job is required.
- Migrating to CDC (Debezium reading the WAL) later means replacing the relay
  only; the table and the write path stay as they are.

---

## ADR-002: PostgreSQL in every environment

**Status:** Accepted — deviates from the project brief

### Context

The brief specifies H2 for development and PostgreSQL for production. The two
speak different SQL dialects, so tests passing against H2 say little about
whether the same query works in production. This project also relies on
Postgres-specific behaviour that H2 does not reproduce: `FOR UPDATE SKIP LOCKED`
in the outbox relay, partial indexes, and `JSONB`.

### Decision

PostgreSQL everywhere. Local development runs a single Postgres container with
one database per service; tests use Testcontainers. H2 is not a dependency of
any module.

Schema is managed by Flyway from the first migration, with
`spring.jpa.hibernate.ddl-auto=validate`.

### Consequences

- Running tests requires a working Docker daemon, locally and in CI.
- Tests are slower than they would be against an in-memory database.
- The behaviour verified in tests is the behaviour shipped to production.

---

## ADR-003: No shared module

**Status:** Accepted

### Context

Several pieces of code will be identical across services: security
configuration, exception handling, and the outbox relay together with its
`OutboxRecord` entity. Extracting them into a shared library removes that
duplication.

The failure mode of a shared library between microservices is well known: it
starts as infrastructure and accumulates integration event DTOs and domain
types. Once that happens the services are compile-time coupled and can no longer
be deployed or versioned independently, which defeats the point of the
architecture.

### Decision

No shared module. Common infrastructure is duplicated across services on
purpose. Each service defines its own integration event DTOs, including those it
only consumes.

If a shared module is ever introduced, one rule is absolute: **no domain types
and no integration event DTOs inside it**, only mechanism.

### Consequences

- The outbox relay is written three times (~80 lines each). Accepted.
- A fix to the relay must be applied in three places.
- Producer and consumer DTOs can drift silently. Contract testing (Spring Cloud
  Contract or Pact) is the intended mitigation and is a required curriculum
  topic.
- No service can break another at compile time.

---

## ADR-004: Transport for saga commands

**Status:** Open

### Context

The booking saga issues commands that require a reply — block seats, charge
payment, and their compensating actions. Commands are not events: the sender
requires a specific receiver to act and needs to know the outcome to advance the
state machine.

Options under consideration:

- **Synchronous Feign from the orchestrator.** The saga reads as sequential
  code, failures surface immediately, compensation is straightforward. Costs
  temporal coupling: the orchestrator blocks while the callee works.
- **Command/reply over Kafka.** Reply topics plus correlation ids. Fully
  decoupled, at the cost of significant machinery for correlation, timeouts and
  saga state rehydration.

Note this is independent of ADR-001: integration events go over Kafka either
way.

### Decision

Pending.

---

## ADR-005: JWT issuer

**Status:** Open

### Context

The brief requires JWT authentication with `PASSENGER`, `AGENT` and `ADMIN`
roles, but lists no service responsible for issuing tokens.

Options under consideration:

- **A dedicated `auth-service`** issuing RS256 tokens exposed through a JWKS
  endpoint. With five resource servers, JWKS avoids distributing a shared secret
  across five configuration files.
- **Keycloak in the compose file.** Production-grade and free of custom
  security code, at the cost of another container and a configuration surface
  that is not itself the learning objective.

If we issue our own tokens, note that Spring Security's `JwtTypeValidator`
rejects tokens without a `typ` header by default.

### Decision

Pending.

---

## ADR-006: Hexagonal architecture with one Maven module per layer

**Status:** Proposed — deviates from the project brief, pending instructor approval

### Context

The brief lists "Arquitecturas avanzadas (DDD / Hexagonal)" as explicitly **out
of scope**. This record proposes deviating from that, and should not be treated
as settled until reviewed.

The argument for deviating: the project is a portfolio piece as much as an
exercise, and the layering it produces is the shape most of these services take
in practice.

The argument against, and the reason this is marked Proposed rather than
Accepted: six services times three layers is eighteen Maven modules for a
two-week MVP. Every new dependency has to be placed in the correct POM, and
every refactor that crosses a layer touches several modules.

### Decision

Each service is a Maven aggregator over three modules:

```
flight-service/              packaging pom, inherits spring-boot-starter-parent
├── flight-domain/           jar — entities, value objects, invariants. No Spring.
├── flight-application/      jar — use cases, ports. No Spring.
└── flight-infrastructure/   jar — adapters, config, entry point. Spring lives here.
```

Dependencies point inwards only: `infrastructure → application → domain`. The
module boundary makes this a **compile-time** guarantee — a domain class simply
cannot import an adapter, because the dependency is not on its classpath.

The entry point sits in `com.programandoenjava.airline.flight`, the package
shared by all three layers, so component scanning reaches adapters across module
jars.

Transaction boundaries are declared in `flight-application` as a
`TransactionRunner` port and implemented in `flight-infrastructure`, which keeps
`@Transactional` out of the application layer.

### Package layout

The rules below encode this layout, so it is part of the decision:

```
com.programandoenjava.airline.flight                       entry point
com.programandoenjava.airline.flight.domain..              flight-domain
com.programandoenjava.airline.flight.application.port.in   use case interfaces
com.programandoenjava.airline.flight.application.port.out  driven ports
com.programandoenjava.airline.flight.application.usecase   implementations
com.programandoenjava.airline.flight.infrastructure.adapter.in.web
com.programandoenjava.airline.flight.infrastructure.adapter.out.persistence
com.programandoenjava.airline.flight.infrastructure.config
```

### Enforcement

Enforced by **ArchUnit tests, not the `maven-enforcer-plugin`**. One mechanism
rather than two, the rules read as documentation in review, and a failure names
the offending class and rule instead of printing a dependency tree.

The rules live in `flight-infrastructure/src/test`, the only module with all
three layers on its classpath. In `flight-domain` ArchUnit would import domain
classes only, and any rule about adapters would pass over an empty set.

What each mechanism covers:

- **The Maven module graph** already makes the inward dependency direction a
  compile error. `flight-domain` cannot import `flight-application` because it
  is not on its classpath.
- **ArchUnit** covers what the compiler cannot see. The important cases are
  entirely inside `flight-infrastructure`: an inbound adapter reaching an
  outbound adapter and bypassing the use case, or a JPA entity escaping the
  persistence adapter. Both compile.
- **Not covered by either:** a banned dependency added to a POM but never
  imported. Enforcer would catch that; ArchUnit only sees usage. Accepted, as an
  unused dependency causes no harm and the first import fails the build.

### Alternative considered

Hexagonal by package inside a single module, with ArchUnit alone enforcing the
direction. Same guarantee, checked in tests rather than by the compiler, without
eighteen modules.

This remains the fallback if the module count proves to cost more than it
returns.

### Consequences

- Eighteen Maven modules once all six services exist.
- The layering rule cannot be violated by accident.
- ArchUnit is still useful for what the compiler cannot see, such as an inbound
  adapter reaching a repository directly and bypassing the use case.
- `spring-boot-maven-plugin` must be declared **only** in the infrastructure
  module. Applied to domain or application, the repackage goal turns those jars
  into fat jars and they silently stop working as dependencies.
