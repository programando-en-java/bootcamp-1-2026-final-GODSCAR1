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
| [011](#adr-011-idempotent-booking-creation) | Accepted | Idempotent booking creation |
| [012](#adr-012-calling-flight-service) | Accepted | Calling flight-service |
| [013](#adr-013-the-payment-saga) | Accepted | The payment saga |
| [014](#adr-014-consuming-a-payment-event-once) | Accepted | Consuming a payment event once |
| [015](#adr-015-a-boarding-pass-without-a-seat) | Accepted | A boarding pass without a seat |
| [016](#adr-016-when-check-in-is-allowed) | Accepted | When check-in is allowed |
| [017](#adr-017-what-a-notification-is-here) | Accepted | What a notification is here |
| [018](#adr-018-carrying-the-passenger-in-a-payment-event) | Accepted | Carrying the passenger in a payment event |

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

`payment`, `checkin` and `booking` own an outbox. The claim that `booking`
would never need one was wrong, and EPIC-05 is where it broke: a booking having
been made is something only `booking` can say, and notifying a passenger about
it is the first thing that had to hear it. `flight` only responds to commands,
and `auth` and `notification` publish nothing.

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

PostgreSQL everywhere, and one instance per service rather than one instance
carved into databases. Each service's compose entry brings up a container of its
own, with its own volume, reachable only under its own network alias. Tests use
Testcontainers, and the end-to-end module arranges the same shape.

H2 is not a dependency of any module.

Schema is managed by Flyway from the first migration, with
`spring.jpa.hibernate.ddl-auto=validate`.

### Consequences

- Running tests requires a working Docker daemon, locally and in CI.
- Tests are slower than they would be against an in-memory database.
- The behaviour verified in tests is the behaviour shipped to production.
- A service physically cannot read another's tables. Sharing an instance made
  that a convention rather than a fact, and a convention is what a stray join
  crosses on a Friday. The cost is one container per service on a laptop, and a
  host port each so both stay reachable.
- There is no longer a script carving databases out of one instance: each
  container creates the one it needs through `POSTGRES_DB`.

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

### End to end

A fourth layer, above the slices: the two services in containers of their own,
each with its own database, talking over a network. It is the only place the
Feign error decoder runs, because every other test in the repository mocks the
port it sits behind — and the only place the idempotency key is seen to travel
as a header rather than assumed to.

That distinction earned itself immediately. booking-service was answering 502
where flight-service had said 422, because the decoder compared `HttpStatus`
constants and Spring 7 deprecated `UNPROCESSABLE_ENTITY` in favour of
`UNPROCESSABLE_CONTENT`, following the rename in RFC 9110. `resolve()` returns
the new constant, the branch never matched, and every other test passed. The fix
is to compare the number, which does not get renamed; the general lesson is that
a status crossing a service boundary is a number, and comparing it as anything
else couples two services to a constant name neither of them owns.

**The stack starts itself**, building images from the same Dockerfiles the
compose file uses, so what is exercised is what would be deployed. The
alternative — running both applications in one JVM — would have been faster to
write and would have put every service's dependencies on one classpath, which is
a montage that exists nowhere else and fails in ways that teach nothing.

**They are skipped unless asked for.** Two images and four containers cost about
a minute, and a suite people run every few minutes is worth more than one that
covers a little extra. `-Dairline.e2e=true` is what turns them on, and CI is
where they always run.

**The tests insert the flights they need.** There is no API for creating one, and
the seed the slices use lives in test resources, so a running instance starts
with an empty catalogue. Each test makes its own with a fresh id rather than
sharing a fixture, because a fixture shared across a network is a fixture nobody
can reset.

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
- The end-to-end tests need a `package` before they run, because the Dockerfiles
  copy a jar Maven has already built. Nothing checks that, so a stale jar is
  tested silently.
- None of this is enforced. It is a convention, and review is what applies it.

---

## ADR-011: Idempotent booking creation

**Status:** Accepted

### Context

`POST /bookings` takes seats off a flight and writes a row. A passenger who
double-clicks, or a client that retries a request whose response was lost, must
not end up with two bookings — and worse, must not leave a second set of seats
held that nobody will pay for.

flight-service solved the same problem with a lookup taken under a pessimistic
lock on the flight row (ADR-007). That approach does not transfer: the row a
booking would lock is the one it is trying to create, so there is nothing to
lock before the decision.

### What the full pattern looks like

The industry answer, which Stripe popularised, is a table of keys separate from
the resource:

- A row is inserted for the key **before any work starts**, marked in progress.
  That insert is what wins or loses the race.
- The response is stored on the row when the work completes, and a retry is
  answered with those same bytes — status code included — rather than by
  rebuilding it from the aggregate.
- A caller that loses the race while the winner is still working is told so, and
  retries rather than being handed a half-finished result.
- Keys expire, typically after a day, or the table grows without bound and a key
  from last year blocks a legitimate booking that happens to reuse it.

### Decision

Not that. Three narrower mechanisms, of which only the last is a guarantee:

**A lookup by key** before anything else, which answers the ordinary case — a
retry seconds later — without troubling flight-service.

**The key forwarded to flight-service as its own key**, alongside a booking id
that stays random. Their contract keeps the two apart — the id answers "has this
booking already taken seats", the key answers "have I already served this
request" — and passing ours through means a race that gets past the lookup is
one request in their eyes too. No second set of seats is held.

Deriving the booking id from the key would have achieved the same and made an
ordinary `save` idempotent as well. It was rejected because a resource id is
opaque everywhere else: it reaches URLs, emails and eventually a passenger, and
one derivable from another value ties two systems together by a relationship
nobody declared. It would also mean that reusing a key after an expiry — which
this design will need — produced a booking wearing the old one's id.

**A unique index on the key**, and a `saveIfNew` built on
`INSERT ... ON CONFLICT DO NOTHING`. The check and the write are one statement,
so there is no window between the question and the answer — which matters
because there is no row to lock beforehand: the row is what the two requests are
competing to create.

The cost is a native query with twelve parameters where a `save` would have taken
an object. The alternative, catching the constraint violation, needs a flush and
a fresh transaction to read afterwards, because Postgres aborts the current one
on a violation.

### Consequences

- Two simultaneous requests both do the work; the loser discards it and returns
  the winner's booking. The full pattern would have the loser wait or be told to
  retry. Accepted: the work is one HTTP call that flight-service answers
  idempotently, so repeating it costs latency and nothing else.
- The response is rebuilt from the stored booking rather than replayed. Identical
  today because nothing in the response varies; it would stop being identical if
  the response ever carried something the aggregate does not hold.
- **Keys are never purged.** The column grows with the table, and a key reused
  after any length of time returns the original booking. A retention job is the
  missing piece, and is the first thing to add if this ever runs for real.
- Reusing a key with a different body returns the original booking rather than
  reporting the mismatch. Detecting it would mean storing a hash of the request
  and comparing, which is machinery for a case only a misbehaving client can
  reach.
- The insert is the one statement in this service written as native SQL. Every
  other query is JPA, and the comment on `insertIfAbsent` is what stops someone
  tidying it into a `save`.

---

## ADR-012: Calling flight-service

**Status:** Accepted

### Context

Creating a booking means asking flight-service for seats over HTTP (ADR-004).
Three things follow from that call being synchronous and across a network: what
happens to its errors, what happens when it is slow or absent, and where the
transaction boundary sits.

### Decision

**Errors are translated by an `ErrorDecoder`,** not caught around the call site.
Feign raises `FeignException.Conflict` for a 409 and `FeignException` for
everything else; neither means anything to a use case. The decoder is where
Feign expects that translation to live, and putting it there keeps the adapter a
single line of delegation.

Refusals are forwarded with the status flight-service chose, because it is the
service that knows why. A 409 means the seats are gone and a 422 means the
flight has, and rewriting either into a generic 502 would tell the passenger
less than the truth. What is not forwarded is flight-service's `detail` text
verbatim: it names a flight by an id the passenger never supplied, so
booking-service phrases its own.

**Retries cover technical failures only.** A 409 will not become a 201 by asking
again — the seats are sold — and a 422 will not either, since the flight has
departed. Retrying them would spend three attempts and three times the latency
to arrive at the same refusal, and would make a busy flight look like an outage.
Resilience4j is configured to ignore those two and retry connection failures,
timeouts and 5xx.

**The circuit breaker is on the client, and its open state is a failure, not a
fallback.** There is no sensible booking to make without seats, so a fallback
that returned one would be inventing a reservation. When the breaker opens,
booking-service says it cannot serve the request.

**No transaction spans the call.** `CreateBookingService` carries no
`@UnitOfWork`, unlike its counterpart in flight-service: the only local write is
the final save, and holding a connection open across an HTTP round trip is how a
slow dependency becomes an exhausted pool.

**Persistence is JPA, except for the insert.** `saveIfNew` needs
`INSERT ... ON CONFLICT DO NOTHING`, which JPQL cannot express, so that one
statement is native inside the repository. The rest stays mapped, because the
stories after this one — listing a passenger's bookings, moving one to paid —
are ordinary queries and JPA is what flight-service already uses.

### Consequences

- `BlockSeatsRequest` and `SeatBlockResponse` are declared again in
  booking-service. They mirror flight-service's DTOs field for field and are not
  shared (ADR-003), so the two can drift silently. Contract testing is the
  intended mitigation and does not exist yet.
- The Feign adapter and its `ErrorDecoder` are the one piece of this service
  without a test. Covering them properly means either WireMock or a running
  flight-service; the decision was to leave them until an end-to-end test exists,
  and this is the note that says so.
- A booking whose seats were held but whose save then failed leaves a hold in
  flight-service that nobody will claim. Nothing releases it today. The expiry
  sweep that ADR-008 anticipates is what would, and it arrives with payment.

---

## ADR-013: The payment saga

**Status:** Accepted

### Context

Paying for a booking spans three services and cannot be one transaction.
payment-service takes the money, booking-service confirms the booking, and
flight-service either keeps the held seats or takes them back. Any of the three
can fail after the others have already acted, which is what makes this a saga
rather than a sequence.

Two questions had to be settled before any of it could be written: who starts
the payment, and who compensates when it fails.

### Who starts it

**The passenger calls payment-service.** `POST /api/v1/payments` names the
booking and carries the card. US-005 says a passenger pays a booking, and this
is that sentence with nothing added.

The alternatives were rejected for concrete reasons rather than taste.

Having booking-service publish `BookingCreated` and payment-service react would
have been more event-driven, but there are no card details anywhere for it to
react with: the payment would have to charge something already on file, and
nothing is. It would also mean a booking that pays itself, which is not what
either story describes.

Having the passenger call booking-service, which then calls payment over Feign,
keeps booking as the single front door. It was rejected because it makes a
synchronous command out of something that is not competing for anything.
ADR-004 draws that line at contention: seats are contended and go synchronously,
a payment is not and does not.

### The shape

1. The passenger posts a payment naming a booking.
2. payment-service asks booking-service what is owed, so the amount is the one
   on the booking rather than the one the caller typed.
3. It charges, and writes the result.
4. It publishes `payment.succeeded.v1` or `payment.failed.v1` through the outbox
   of ADR-001.
5. booking-service consumes. On success the booking becomes `CONFIRMED`; on
   failure it becomes `FAILED` and the held seats are released through
   flight-service.

Step 2 is not optional. A payment whose amount comes from the request body is a
payment a caller can under-pay, and the booking is the only place that knows
what the fare was when the seats were held.

Step 5 is where the compensation lives, and it lives in booking-service on
purpose. booking is the service that knows which hold belongs to which booking;
payment-service has no business knowing flight-service exists.

### The gateway

There is no real acquirer, so `PaymentGatewayPort` has a stand-in adapter behind
it. **Its outcome is decided by the card number, not by chance**: a card in a
declining range fails, everything else succeeds.

A random adapter would make US-006 untestable in any useful sense — an
end-to-end test that fails one run in ten teaches people to re-run rather than
to read. Deciding on input also means a demonstration can show either path on
purpose.

### Consequences

- **Bookings become eventually consistent.** Between the payment succeeding and
  booking-service consuming the event, a booking is paid and still says
  `PENDING`. Nothing today closes that window, and any read of a booking has to
  be understood as a snapshot rather than a verdict.
- **A failed payment leaves seats held until the compensation runs.** That is the
  point of the compensation, and it is also a window during which those seats
  are sold to nobody. The expiry sweep ADR-008 anticipates is what would bound
  it; the compensation is what makes it short in the ordinary case.
- **payment-service depends on booking-service** to learn the amount, so paying
  is impossible while booking is down. Acceptable: there is nothing to pay for
  if the booking cannot be read.
- **Nothing retries the compensation yet.** If the release call fails, the
  booking is `FAILED` and the seats stay held. Making the consumer retry, and
  making the release idempotent, is what closes that — the second half is done
  (ADR-014), the first is not.
- `BookingStatus` grows from one value to three. `CONFIRMED` and `EXPIRED` were
  deliberately left out when the enum was written, on the grounds that they were
  guesses; US-005 and US-006 are the requirements that stopped them being
  guesses, and `EXPIRED` is still not among them.

---

## ADR-014: Consuming a payment event once

**Status:** Accepted

### Context

ADR-001 makes delivery at-least-once and states flatly that every consumer must
be idempotent. booking-service is the first consumer, so this is where that
requirement stops being a sentence.

A repeat is not an edge case. The relay marks a row sent only after the broker
acknowledges it, so a crash in between resends the message; a rebalance resends
whatever was not committed. Confirming a booking twice is harmless, but
releasing seats twice, or releasing seats for a booking that has since been
confirmed, is not.

### Decision

**A `processed_events` table, claimed before any work.** The event id travels
in the payload from the outbox row that produced it, and the consumer inserts it
with `ON CONFLICT DO NOTHING`. One row inserted means this delivery does the
work; zero means another already did.

Insert-and-see rather than ask-then-insert, for the same reason as the bookings
table: two deliveries can arrive together and a separate lookup would let both
through.

**The claim travels into the use case, not the listener.** The listener parses a
payload and calls a method; what must not happen twice is the work, and only the
use case knows what the work was. A consumer that deduplicated on its own would
also have to know that confirming and failing are different amounts of work.

**The domain tolerates a repeat anyway.** `Booking.confirm()` on a confirmed
booking returns it unchanged rather than complaining, and `fail()` likewise. The
opposite transition still throws: a booking cannot travel both roads. That makes
the table an optimisation for the ordinary path and a guarantee for the rest,
instead of the only thing standing between a redelivery and a broken invariant.

**Failing marks first and releases second.** The booking becomes `FAILED` in a
local transaction; the seats go back over HTTP afterwards, outside it. The order
is the decision: a crash after marking leaves seats held on a booking that says
it failed, which a sweep can find and fix. A crash after releasing would leave a
`PENDING` booking with no seats, which nothing could tell apart from a booking
still waiting.

`seats_released_at` records that the second step finished. It is a column rather
than a fourth status because the status is what a passenger sees, and whether
the compensation completed is the saga's own bookkeeping.

### Consequences

- **Nothing retries the release.** If flight-service is unreachable past
  Resilience4j's attempts, the booking stays `FAILED` with `seats_released_at`
  unset and the seats stay held. The partial index `idx_bookings_awaiting_release`
  exists for the sweep that would fix this; the sweep does not.
- **`processed_events` is never purged.** It grows with every message this
  service consumes. Same gap as the idempotency keys in ADR-011, and the same
  answer: a retention job.
- A message naming a booking that does not exist raises, so Kafka redelivers it
  forever. Nothing sends it to a dead letter topic, which is what should happen
  to a message no amount of retrying will fix.
- The claim is committed in its own transaction, before the work. A crash
  between claiming and finishing means the event is marked handled and the work
  is not done. Doing both in one transaction would fix that for the confirm
  path, and cannot for the fail path, whose second step is a network call.

---

## ADR-015: A boarding pass without a seat

**Status:** Accepted

### Context

US-007 asks for a boarding pass. Every boarding pass anyone has held names a
seat, and this system has none: `SeatInventory` is a total and a number
available, and a seat block is a count against a flight. Nothing anywhere models
the 12A.

Three ways out were considered.

**checkin-service assigns a seat from the flight's capacity.** Rejected. Seat
inventory belongs to flight-service, which guards it with a lock (ADR-007). A
second service inventing seat identity from a number it does not own is the
split brain the bounded contexts exist to prevent, and there would be no way to
reconcile it with the block flight-service is already holding.

**flight-service starts modelling seats.** Correct for a real airline and larger
than epics 2 and 3 together: a seat map per aircraft, a row per seat with a
state, blocks changing from "two seats" to "these two seats", locking per seat,
an endpoint for check-in to claim one, and a compensation that releases them.
It would touch ADR-007, ADR-008 and the whole payment saga. No story asks for
it: choosing a seat is a feature nobody requested.

### Decision

**Open seating. The pass carries a boarding sequence instead of a seat.**

The sequence is one flight's boarding order, and it is a number this service
owns: it knows how many passes it has issued for that flight, which is not true
of anything about seats. A counter row per flight is read for update, so two
passengers checking in at the same moment take turns rather than being handed
the same place.

**The pass keeps a copy of the flight, not a reference to it.** Flight number,
route and departure are written into the row when the pass is printed. A
boarding pass is a document: it records a moment, and showing it again must not
depend on flight-service answering.

### Consequences

- **Nobody can choose where they sit**, and nothing here is a step toward it. If
  seats are modelled later, the pass gains a field and check-in asks
  flight-service for one; none of this has to be undone.
- **The pass names a passenger by uuid**, because no service holds a name. There
  is no auth-service, so `PassengerId` is all a booking carries, and a document
  that cannot be matched to a person is a real limitation rather than a
  simplification.
- **A boarding number can be skipped.** If two requests for the same booking
  arrive together, both take a place and only one pass is written. Nothing
  boards by a number that has to be contiguous, and the alternative is a lock
  held across every check-in on the flight.
- **A rescheduled flight leaves a pass saying the old time.** Nothing tells
  check-in that a departure moved, which is the cost of copying. The alternative
  costs a call to flight-service on every read of a pass.

---

## ADR-016: When check-in is allowed

**Status:** Accepted

### Context

US-008 says check-in must be refused when the booking is not valid, and that the
passenger must be told what the problem is. The first half is a status: only a
`CONFIRMED` booking can be checked in, which check-in learns by reading the
booking the way payment-service does (ADR-013).

The second half is time, and it is why flight-service grew a read endpoint. A
system that lets someone check in for a flight three weeks out, or for one that
left yesterday, is not doing the job the story describes.

### Decision

**A window expressed as two distances from departure**, opening 24 hours before
and closing an hour before, both configurable. The rule is a domain value
object; the two numbers are configuration, so an airline that opens check-in
earlier changes a property rather than code.

**Three refusals rather than one**, because they ask different things of the
passenger: come back later, go to the desk, and the flight has gone. Each is a
separate exception with its own problem type, which is what makes "the user is
informed of the problem" mean something more than a 409.

**`GET /api/v1/flights/{id}` answers for a departed flight**, unlike the search,
which hides them because nobody can buy a seat on one. Check-in needs the
departure precisely in order to explain why it is refusing.

### Consequences

- **checkin-service depends on two services synchronously.** Checking in is
  impossible while either is down. Acceptable: there is nothing to print if the
  booking cannot be read, and nothing to print on it if the flight cannot.
- **The window is measured against the service's own clock**, so a passenger at
  the boundary can be refused by a second. No airline promises otherwise.
- **Nothing consumes `checkin.completed.v1` yet.** The outbox is written and the
  relay sends it because notification-service is the next epic, and the row has
  to share the transaction that printed the pass, which cannot be added
  afterwards from outside.
- **Nobody checks whose booking it is.** The booking id is enough to check in,
  which is the same gap bookings and payments already have, and it closes when
  there is an auth-service to close it.

---

## ADR-017: What a notification is here

**Status:** Accepted

### Context

EPIC-05 asks that a passenger be told when they book, when they pay and when
they check in. Nothing in this system can tell anyone anything: no service holds
an email address, a phone number or a name. `PassengerId` is a uuid and that is
all there has ever been, because there is no auth-service.

So "the user receives a notification" had to be given a meaning that is
honest rather than one that looks finished.

### Decision

**A channel port with an adapter that writes a log line.** The port is what a
real provider would replace; the adapter is what this system can actually do.
The same shape as the payment gateway: a seam that is simulated on purpose, not
a stub that pretends.

**Every send is recorded in `notifications`**, with `sent_at` null until the
channel has taken it. There is no read endpoint: this is the record of a send,
not an inbox, and no story asks for one.

**Written first, sent second, marked third.** A crash between the write and the
send leaves a notification nobody received, which the partial index over
`sent_at IS NULL` makes findable. The other order would send one and forget it
had, and the passenger would get it again on the next delivery.

**Every consumer claims the event id before doing anything** (ADR-014). This
matters more here than anywhere else in the system: confirming a booking twice
is the same booking, and notifying a passenger twice is the thing they notice.

**Three modules, like every other service.** The domain is thin, but the wording
lives in it and is tested without a broker, which is the part of this service
anyone would ever read.

### Consequences

- **Nobody is actually told anything.** A log line is not a notification, and
  the day this system has contact details it will need a real adapter and a
  retry policy that goes with it.
- **Nothing sweeps the unsent.** The index exists, the job does not. A channel
  that throws leaves a row that stays unsent until somebody looks.
- **The channel is called outside the transaction**, so a provider that hangs
  cannot hold a database connection open. The cost is the window above.
- **`processed_events` is never purged**, the same gap booking-service has.
- **A booking notification cannot name the flight.** booking-service holds seats
  by id and never learns the flight number, so the message names the booking and
  the total instead. Only the check-in message can say which aeroplane.

---

## ADR-018: Carrying the passenger in a payment event

**Status:** Accepted

### Context

`payment.succeeded.v1` carried the payment, the booking and the amount, and no
passenger. payment-service did not even know who the passenger was: it reads the
booking to find out what is owed and discarded every other field.

To notify anyone about a payment, something had to supply the passenger. Three
ways were considered.

**notification-service reads booking-service over HTTP** when a payment message
arrives. Rejected: a synchronous call inside the consumption of a message means
that when booking-service is down the consumer either blocks its partition
retrying or drops the notification, and it would need Feign and a resilience
configuration to do it.

**notification-service remembers who a booking belongs to** from
`booking.created.v1`, which it consumes anyway. Rejected, and this is the
interesting one: the two are different topics, and Kafka orders messages within
a partition, not across topics. In production minutes pass between booking and
paying, so it would almost always work. In the end-to-end test milliseconds
pass. A failure that only appears when things are fast is the kind that appears
in a demonstration.

### Decision

**The event carries the passenger.** `BookingToPay` keeps the `passengerId` that
booking-service already returns, `PayBookingService` hands it to the recorder,
and `PaymentSettled` carries it to the listener that writes the outbox row.

**`Payment` does not store it.** No column, no migration, no field. A payment is
money, and whose booking it settles is booking-service's business. The passenger
travels beside the payment inside the in-process event, because an announcement
is addressed to someone even when the thing being announced is not.

**Both payment events get it**, not just the successful one. Nobody notifies on
a refusal today, and building the two contracts differently would be a
difference with no reason behind it.

### Consequences

- **The contract stayed at v1.** Adding a field does not break a consumer that
  ignores what it does not know, and booking-service's copy of this event
  declares two fields and says so in its own javadoc. A rename or a removal
  would need v2.
- **A payment message now names a passenger**, which is one more place a
  passenger identifier exists. With real personal data rather than a uuid that
  would be worth weighing; with a uuid it is not.
- **payment-service reads a field it does not use itself.** If booking-service
  ever stops returning it, payment breaks for a reason that has nothing to do
  with payments.
