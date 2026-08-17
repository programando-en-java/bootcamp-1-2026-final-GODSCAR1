# End-to-end tests

The stack is brought up by the tests themselves: three containers on a network
of their own, built from the same Dockerfiles the compose file uses.

```
./mvnw -B clean package
./mvnw -B verify -pl e2e-tests "-Dairline.e2e=true"
```

The `package` first is not optional. The Dockerfiles copy a jar Maven has
already built, so without it the images carry whatever was there before.

The flag is what keeps them off an ordinary `./mvnw verify`. Building two images
and starting three containers costs about a minute, and a suite people run every
few minutes is worth more than one that covers a little extra.

## What they cover that nothing else does

The Feign error decoder in booking-service. Every other test in the repository
mocks the port it sits behind, so the translation from a 409 out of
flight-service into a conflict out of booking-service runs here and nowhere
else — which is how a decoder that missed 422 and answered 502 was found.

The same goes for the idempotency key reaching flight-service as a header: until
the two services actually speak, that is an assumption.

## Flights

The tests insert the flights they need straight into `airline_flight`. There is
no API for creating a flight, and the seed the slice tests use lives in test
resources, so a running instance starts with an empty catalogue.

Each test inserts its own flight with a fresh id, so they neither collide nor
depend on each other's leftovers.
