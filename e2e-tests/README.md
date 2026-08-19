# End-to-end tests

The stack is brought up by the tests themselves: three databases, a broker and
the three services, built from the same Dockerfiles the compose file uses.

```
./mvnw -B clean package
./mvnw -B verify -pl e2e-tests "-Dairline.e2e=true"
```

The `package` first is not optional. The Dockerfiles copy a jar Maven has
already built, so without it the images carry whatever was there before.

The flag is what keeps these off an ordinary `./mvnw verify`. Building three
images and starting seven containers costs a few minutes, and a suite people run
every few minutes is worth more than one that covers a little extra.

## What they cover that nothing else does

**The Feign decoders.** Every slice mocks the port a decoder sits behind, so the
translation from a 409 out of flight-service into a conflict out of
booking-service runs here and nowhere else — which is how a decoder that missed
422 and answered 502 was found.

**The saga.** A payment settles a booking through a Kafka message, and a refused
payment sends seats back to flight-service over HTTP. Both cross three services;
neither can be shown by anything smaller.

Settling is asynchronous, so those assertions poll rather than checking once.

## Flights

The tests insert the flights they need straight into `airline_flight`. There is
no API for creating one, and the seed the slice tests use lives in test
resources, so a running instance starts with an empty catalogue.

Each test makes its own with a fresh id, so they neither collide nor depend on
each other's leftovers.
