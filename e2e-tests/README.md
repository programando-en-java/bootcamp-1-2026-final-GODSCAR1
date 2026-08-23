# End-to-end tests

The stack is brought up by the tests themselves: a database per service, a
broker and the six services, built from the same Dockerfiles the compose file
uses.

```
./mvnw -B verify -pl e2e-tests "-Dairline.e2e=true"
```

No `package` first. The Dockerfiles build the jar themselves in a first stage,
so an image can no longer carry a jar from an earlier run. The cost is that a
run compiles inside Docker: the dependency layer is cached until a pom changes,
but the sources are compiled again every time.

The flag is what keeps these off an ordinary `./mvnw verify`. Building six
images and starting fourteen containers costs several minutes, and a suite
people run every few minutes is worth more than one that covers a little
extra.

## What they cover that nothing else does

**The Feign decoders.** Every slice mocks the port a decoder sits behind, so the
translation from a 409 out of flight-service into a conflict out of
booking-service runs here and nowhere else, which is how a decoder that missed
422 and answered 502 was found.

**The saga.** A payment settles a booking through a Kafka message, and a refused
payment sends seats back to flight-service over HTTP. Both cross three services;
neither can be shown by anything smaller.

Settling is asynchronous, so those assertions poll rather than checking once.

**Check-in reading two services.** Its slice mocks both ports, so that
booking-service answers with a status check-in understands, and flight-service
answers with a departure it can measure its window against, is shown here alone.
The window is also the reason those flights depart in hours rather than days.

**Notifications.** Three services announce three things on three topics and
notification-service reads all of them. Nothing calls it, so there is no other
way to find out whether what those services publish is what it expects. Every
assertion there polls: a notification exists once a relay has swept, the broker
has delivered and the consumer has run, none of which has happened by the time
the request that caused it has answered.

## Flights

The tests insert the flights they need straight into `airline_flight`. There is
no API for creating one, and the seed the slice tests use lives in test
resources, so a running instance starts with an empty catalogue.

Each test makes its own with a fresh id, so they neither collide nor depend on
each other's leftovers.
