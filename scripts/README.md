# Smoke scripts

Three walks through the running system, against the stack `docker-compose.yml`
brings up. They are development tooling, not tests: nothing in CI runs them, and
nothing fails if they are never used.

```powershell
docker compose up -d --build --wait

.\scripts\smoke-payment.ps1              # US-005: pay a booking, saga confirms it
.\scripts\smoke-payment.ps1 -Decline     # US-006: card refused, seats go back
.\scripts\smoke-checkin.ps1              # US-007: check in, get a boarding pass
.\scripts\smoke-checkin.ps1 -TooEarly    # US-008: window shut, nothing issued
.\scripts\smoke-notifications.ps1        # US-009 to US-011: three notifications

docker compose down -v
```

## One door

Every request goes to `http://localhost:8080`. The services are no longer
published: the gateway is the only way in, and seat blocks are not reachable
through it at all (ADR-024).

## Logging in

Every call to a service needs a token now, so each script starts by logging in
as a seeded demo account and carries the bearer from there. The passenger is
whoever it logged in as: no request can name one any more (ADR-021).

| Account | Password | Role |
| --- | --- | --- |
| `passenger@airline.test` | `passenger123` | PASSENGER |
| `agent@airline.test` | `agent123` | AGENT |
| `admin@airline.test` | `admin123` | ADMIN |

They are seeded by a migration and are nobody's idea of a secret. There is no
sign up, because no story asks for one.

## Why they exist when there is an end-to-end suite

`e2e-tests` starts its own stack with Testcontainers, building the images from
the same Dockerfiles but wiring them itself: it passes each service its database
url and its broker address directly. That is what makes it reproducible, and it
is also what makes it blind to `docker-compose.yml`.

checkin-service went from EPIC-04 to EPIC-05 with no broker address in the
compose file. Its outbox filled up and nothing ever left. Every test passed, in
every module and end to end, because none of them read that file. The check-in
smoke script is what found it.

So: the suite proves the services work together. These prove the thing you
actually run works.

## Windows only

They are PowerShell, because that is the shell this project is developed in.
Nothing else in the repository depends on them, and a port to bash would be
welcome for anyone working elsewhere.

## What they assume

The stack is already up. Each checks the containers it needs before touching
anything, because a failed `docker exec` only sets `$LASTEXITCODE` and the
script would otherwise carry on and report a flight it never created.

Each starts by inserting a flight straight into flight-service's database with
`docker exec`, because there is no endpoint that creates one. Departures are set
relative to now, since check-in measures its window against the clock inside the
container.

They read results out of the databases with `psql`, and out of Kafka with the
console consumer, whose `--timeout-ms` expiry prints an error and a
`Processed a total of N messages` line. That error is how the read ends, not a
failure.


Each starts by inserting a flight straight into flight-service's database with
`docker exec`, because there is no endpoint that creates one. Departures are set
relative to now, since check-in measures its window against the clock inside the
container.

They read results out of the databases with `psql`, and out of Kafka with the
console consumer, whose `--timeout-ms` expiry prints an error and a
`Processed a total of N messages` line. That error is how the read ends, not a
failure.
