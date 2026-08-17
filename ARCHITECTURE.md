# Architecture

Architecture Decision Records for the airline reservation system.

Each record states the context, the decision, and the consequences we accept.
Records marked **Open** are not yet decided and are pending discussion.

| ADR | Status | Decision |
|-----|--------|----------|
| [001](#adr-001-transactional-outbox-for-integration-events) | Accepted | Transactional outbox for integration events |
| [002](#adr-002-postgresql-in-every-environment) | Accepted | PostgreSQL in every environment, no H2 |
| [003](#adr-003-no-shared-module) | Accepted | No shared module; deliberate duplication |
| [004](#adr-004-synchronous-commands-for-contended-resources-kafka-for-facts) | Accepted | Synchronous commands for contended resources, Kafka for facts |
| [005](#adr-005-jwt-issuer) | Open | JWT issuer |
| [006](#adr-006-hexagonal-architecture-with-one-maven-module-per-layer) | Accepted | Hexagonal architecture, one Maven module per layer |
| [007](#adr-007-pessimistic-locking-for-seat-inventory) | Accepted | Pessimistic locking for seat inventory |
| [008](#adr-008-seat-blocks-as-a-separate-aggregate) | Accepted | Seat blocks as a separate aggregate |
| [009](#adr-009-transaction-boundaries-declared-with-an-annotation) | Accepted | Transaction boundaries declared with an annotation |
| [010](#adr-010-testing-style) | Accepted | Testing style |

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

## ADR-004: Synchronous commands for contended resources, Kafka for facts

**Status:** Accepted

### Context

Booking a flight takes seats off a shared inventory. Two passengers can reach
for the last seat at the same instant, and the loser has to be told immediately:
a passenger who is told "we are working on it" is a passenger who books
elsewhere.

The options were synchronous Feign from the caller, or command/reply over Kafka
with reply topics and correlation ids. The second decouples the two services
fully, at the cost of machinery for correlation, timeouts, and rehydrating a
caller that may no longer be running when the reply arrives.

A third framing settled it. Real airline systems do both, in sequence: the seat
is held synchronously, because contention has to be resolved now, and everything
downstream — payment, ticket issue, notification — is asynchronous, because
nothing is waiting on it. US-003 and US-004 are entirely the first half.

### Decision

Seat blocking is a synchronous call from booking-service to flight-service,
resolved inside the request that created the booking. Everything downstream of a
confirmed booking travels over Kafka.

The dividing line is whether the message competes for a scarce resource.
Commands that do go synchronously, because contention has to be resolved now and
the caller has to hear the answer. Facts that have already happened go over
Kafka, because nothing blocks on them and at-least-once delivery is enough.

This is independent of ADR-001: integration events go over Kafka either way.

### Consequences

- booking-service cannot create bookings while flight-service is down. That is
  correct rather than unfortunate: without an inventory to draw from there is no
  booking to make, and a booking accepted on faith is one that may have to be
  revoked. Resilience4j gives the failure a definite shape instead of a hung
  request.
- Seat blocking becomes a write endpoint on flight-service, which until now was
  read-only. ADR-007 applies to it.
- This decision covers US-003 and US-004 only. Payment introduces a genuine
  saga — a failed payment has to release the seats it reserved — and that
  compensation travels over Kafka.

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

Until this is settled, endpoints needing a passenger identity take it from the
request body. US-013 replaces that with a token, and the value objects carrying
those identities are shaped so that only their source changes.

### Decision

Pending.

---

## ADR-006: Hexagonal architecture with one Maven module per layer

**Status:** Accepted — deviates from the project brief, approved by the instructor

### Context

The brief lists "Arquitecturas avanzadas (DDD / Hexagonal)" as explicitly **out
of scope**. This record deviates from that.

The argument for deviating: the project is a portfolio piece as much as an
exercise, and the layering it produces is the shape most of these services take
in practice.

The argument against: six services times three layers is eighteen Maven modules
for a two-week MVP. Every new dependency has to be placed in the correct POM,
and every refactor that crosses a layer touches several modules.

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

The rules below encode this layout, so it is part of the decision. **The paths
are prefixes**: packages may nest below them, grouping by aggregate in the
domain and by use case in the application layer. The ArchUnit rules match any
depth.

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

As it stands in flight-service:

```
domain/flight/        Flight, FlightId, FlightNumber, FlightSchedule, SeatInventory
domain/seatblock/     SeatBlock, SeatBlockId, SeatCount, BookingId, SeatsBlocked
domain/shared/        AirportCode, Money, DomainValidationException

application/port/in/searchflights/    SearchFlightsQuery, SearchFlightsUseCase
application/port/in/blockseats/       BlockSeatsCommand, BlockSeatsUseCase
application/port/in/shared/           PageQuery, PageResult, SortableField, IdempotencyKey
application/port/out/searchflights/   LoadFlightsPort, FlightSearchCriteria
application/port/out/blockseats/      BlockSeatsPort, FindSeatBlockPort
application/usecase/                  SearchFlightsService, BlockSeatsService
```

Note that `IdempotencyKey` sits in the application layer rather than the domain.
An airline has no notion of idempotency keys; the concept exists because HTTP
cannot distinguish a lost response from an unhandled request.

### What counts as a framework

The inner modules are framework-free, and the rule needs a criterion sharper
than a list of banned packages, because the next borderline dependency will not
be on that list.

An annotation may live in `flight-domain` or `flight-application` if it
**describes** code that is already written. It may not if it **generates** code,
changes an object's lifecycle, or needs a runtime to interpret it.

JSpecify passes. `@NullMarked` and `@Nullable` have CLASS retention, nothing
loads them, and the compiled output is byte-identical with or without them. They
say what the constructors already enforce, in a form the compiler and the IDE
can check. They are declared `provided` in all three modules, since they are
needed to build and never to run, and `provided` does not travel down the
dependency graph.

Lombok fails the first clause. `@Data` writes getters, setters, equals and
hashCode that never appear in the source, and the setters alone would undo the
immutability ADR-008 depends on. It is also unnecessary here: records already
generate everything it would.

Bean Validation fails the third. `@NotNull` does nothing until a `Validator`
reads it, so the guarantee lives in a runtime rather than in the type. Input
validation stays at the web adapter, where that runtime already exists.

MapStruct fails the first, for the same reason as Lombok.

**The annotations document, they do not enforce.** Nothing in the build checks
them today: a null passed where `@NullMarked` forbids one still compiles.
NullAway on the compiler plugin would make them binding, and is worth adding
once the existing code has been audited for what it would surface.

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

---

## ADR-007: Pessimistic locking for seat inventory

**Status:** Accepted

### Context

Seat availability is the one number in this system that requests genuinely fight
over. Two bookings for the last two seats on a flight arrive at the same
instant, and both must not succeed.

Optimistic locking with a `@Version` column detects the conflict after the fact:
the second transaction fails at commit and the caller retries. Under contention
that becomes a retry storm, and the seats a passenger saw are seats they may
still lose after submitting.

### Decision

Pessimistic locking. The flight row is read `FOR UPDATE` at the start of the
blocking transaction, so a second request waits rather than failing. There is no
`@Version` column on `FlightEntity`.

The database keeps the last word regardless. `V1__create_flights.sql` carries
`chk_flights_seats_within_capacity CHECK (available_seats BETWEEN 0 AND
total_seats)`, so a write that somehow bypassed the lock still cannot oversell
or over-release. That constraint mirrors the invariant in `SeatInventory` on
purpose: the record protects the application, the constraint protects the data
against anything that writes to the table without going through it.

### Consequences

- Concurrent bookings for the same flight serialise. That is the point; the
  contended resource is one row and the critical section is short.
- A lock timeout has to be configured, or a stuck transaction blocks every other
  booking on that flight indefinitely.
- Locks are taken only on flights being booked. Searching does not lock, so the
  read path is unaffected.
- This works because seats and blocks live in one database. Splitting them
  across services would leave nothing to lock, and the guarantee would have to be
  rebuilt as a saga with compensation.

---

## ADR-008: Seat blocks as a separate aggregate

**Status:** Accepted

### Context

Holding seats for a booking changes two things at once: the flight loses seats,
and a record appears saying who took them. The question is whether that record
belongs inside `Flight` or stands on its own.

Its lifecycle argues for standing on its own. A flight exists for months; a hold
lasts minutes and then either becomes a confirmed booking or expires. Expiry
will be a sweep over holds, not over flights, and it will touch one hold at a
time. Modelling holds as a collection inside `Flight` would mean loading every
hold ever taken on a flight in order to add one.

But separating them raises the question the usual guidance answers with "one
aggregate per transaction": if the seat count on the flight and the sum of
outstanding holds are two numbers, they can disagree.

### Decision

`SeatBlock` is its own aggregate, and blocking writes both aggregates inside one
transaction, under the lock from ADR-007.

`Flight.block` returns `SeatsBlocked`, a value carrying the reduced flight and
the block that reduced it. Both come back together because a caller that kept
one and dropped the other would have either lost the seats or sold them twice —
and since `Flight` is immutable, discarding the returned flight is a mistake the
compiler cannot see.

The alternative considered was dropping `available_seats` altogether and
deriving availability as `capacity - SUM(held seats)`. That makes disagreement
impossible, because there is one number rather than two. It was rejected because
the aggregate would then run on every search — by far the most frequent
operation — to protect a write path that a lock already serialises.

### Consequences

- One transaction spans two aggregates. This departs from the usual rule, and is
  accepted because both live in the same database and the lock already
  serialises the write. It would not survive splitting flights and holds across
  services.
- Anything that changes seat availability must go through `Flight.block` or
  `Flight.releaseSeats` and persist what comes back. A raw
  `UPDATE flights SET available_seats` elsewhere breaks the correspondence
  silently.
- The correspondence is an invariant no type enforces. A reconciliation query —
  comparing `available_seats` against capacity minus outstanding holds — is how
  drift would be detected, and is worth having before this is trusted in
  production.

---

## ADR-009: Transaction boundaries declared with an annotation

**Status:** Accepted

### Context

ADR-006 keeps `@Transactional` out of the application layer, which leaves the
question of where a transaction begins. Two places were tried and both were
wrong.

Putting it on the persistence adapter, as `@Transactional(readOnly = true)`,
works while a use case makes one port call. Blocking seats makes four — read the
flight under lock, look for an existing hold, check the booking, write both
aggregates — and with the boundary on the adapter each of those opens and closes
its own transaction. The lock would be released the moment the flight was read,
and the endpoint would oversell.

Moving the whole operation behind a single port method fixes that, but at the
cost of the adapter deciding whether a booking may hold seats and throwing an
application exception to say no. Business rules in the persistence layer.

### Decision

`@UnitOfWork` is declared in `flight-application`, and an aspect in
`flight-infrastructure` implements it by delegating to a `TransactionRunner`.

```java
@Retention(RUNTIME) @Target(METHOD)
public @interface UnitOfWork { boolean readOnly() default false; }
```

The annotation imports nothing from any framework. It states a need; the runner
meets it. That is the port and adapter relationship, written as an annotation
instead of an interface — which is why `TransactionRunner` lives in
infrastructure rather than beside the annotation: nothing in the application
layer calls it.

With the boundary on the use case, the ports go back to being granular
(`LockFlightPort`, `SaveFlightPort`, `FindSeatBlockPort`, `SaveSeatBlockPort`),
the adapters go back to translating, and `BlockSeatsService` reads as the
operation it performs, with the lock held from the first line to the last.

`@UnitOfWork(readOnly = true)` on the search path is closer to decorative — one
query would run in its own transaction anyway — but it sets the flag before the
query rather than after, and it states the boundary in the same place for both
use cases instead of leaving one visible and the other implicit.

### Consequences

- **Being proxy-based, it only fires on calls arriving from outside the bean.**
  The annotation goes on the method implementing the inbound port, never on one
  the service calls on itself. A use case built with `new` in a test has no
  transaction at all, and nothing announces it.
- `@EnableAspectJAutoProxy` is declared explicitly in
  `TransactionSupportConfiguration`. Boot would normally do it, but a hand-built
  slice runs with auto-configuration off, and without auto-proxying the aspect
  never fires: every annotated method runs outside a transaction, the seat lock
  stops being held to commit, and the tests still pass. Every slice that touches
  a use case lists that configuration for this reason.
- Use cases are registered as their inbound port type so the JDK can build an
  interface proxy. Returning the concrete class would work through CGLIB, but
  only while the class stays non-final with a usable constructor — a fragile
  thing for a transaction to depend on.
- `ConcurrentSeatBlockTest` is the only test that fails if the annotation is
  removed. Without it the mechanism would be unverified, and its absence would
  surface as overselling in production rather than as a red build.
- `RUNTIME` retention is mandatory. With `CLASS` the annotation never reaches the
  aspect and everything runs untransacted, silently.
- `spring-boot-starter-aspectj` is required — Boot 4 renamed
  `spring-boot-starter-aop`.

---

## ADR-010: Testing style

**Status:** Accepted

### Context

The suite has to survive people changing the code, which means it has to fail
for the right reasons. A test that breaks when a price moves in a fixture is
worse than no test: it costs attention every time and teaches the team to re-run
rather than to read.

### Decision

**Slices over unit tests, except for pure logic.** A hand-built Spring context —
the classes that compose a request, listed by name, with auto-configuration off
— exercises the code the way production runs it. Unit tests are kept for classes
that decide something in isolation: the domain records, and
`SearchFlightsService`, whose date clamping is a calculation over a fixed clock.

Coordination is deliberately not unit tested. A mocked `BlockSeatsService` would
assert the order of its port calls while proving nothing about the lock that
makes that order correct, and would keep passing if `@UnitOfWork` were removed.

**One test per suite may be coupled to the fixture; the rest may not.**
`shouldListTheBookableCatalogue` names all six flights in order, so that changing
the seed fails somewhere that says so. Every other test asserts a property —
every origin is BOG, the prices descend, two fewer seats than before — and
survives a fixture that grows.

**Read the fixture for identity and for prior state, never for the expected
result.** Looking up a flight by number rather than copying its uuid is a
reference. Reading the seat count before an operation and asserting the
difference is a fact about the change. Computing how many flights the search
should return would be re-implementing the query under test, and a test that
recalculates the answer agrees with the bug it was meant to catch.

**No literals where a name exists.** Status codes, seat counts, JSON paths, SQL
and request parameters are constants. Where a bound already exists in production
code it is read from there: `SeatCount.MAX + 1` rather than `10`,
`PageQuery.MAX_SIZE + 1` rather than `100`.

**`@Nested` with `@DisplayName`, methods named `should…`,**
`Assertions.assertThat` and `BDDMockito.given` as prefixed references rather than
static imports, and DAMP over DRY: a test that repeats its own setup is easier to
read than one that inherits it.

**Fixtures are restored, not rolled back.** Both slices replay the seed with
`@Sql` before each method. A `@Transactional` test would roll back instead, but
its transaction would nest inside the one `@UnitOfWork` opens and the pessimistic
lock would stop behaving as it does in production — which is the behaviour the
seat block tests exist to check. The replay also keeps the two slices
independent: they share a cached context and therefore a database, and one of
them writes.

**Time is pinned.** Both slices fix the clock to `2026-03-10T12:00Z` with
`@TestBean`, which is what lets the seed use literal dates instead of offsets
from today. Relative dates would leave the test and the database each working out
what "today" is — an agreement that holds for most of the day and not all of it.

### Consequences

- The fixed instant lives in three files: the seed and both slices. Changing one
  without the others fails loudly, which is the acceptable kind of coupling.
- Duplicating the clock declaration in each slice is deliberate. Extracting it to
  a base class would hide the one piece of setup a reader most needs to see.
- `ConcurrentSeatBlockTest` is slow and is the test that earns US-004 its claim.
  The rest of the suite runs one request at a time and cannot tell a held lock
  from no lock at all.
- Endpoint paths are written out in the tests rather than read from the
  controller's annotations. That duplication is the point: a test that sourced
  the path from the code under test would stay green while the route changed
  under every client.
- None of this is enforced. It is a convention, and review is what applies it.