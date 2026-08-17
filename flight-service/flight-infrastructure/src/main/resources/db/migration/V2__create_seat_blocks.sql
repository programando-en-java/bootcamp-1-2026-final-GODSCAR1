CREATE TABLE seat_blocks (
                             id               UUID         PRIMARY KEY,
                             flight_id        UUID         NOT NULL,
                             booking_id       UUID         NOT NULL,
                             seats            INTEGER      NOT NULL,
                             idempotency_key  VARCHAR(255) NOT NULL,
                             blocked_at       TIMESTAMPTZ  NOT NULL,

                             CONSTRAINT fk_seat_blocks_flight
                                 FOREIGN KEY (flight_id) REFERENCES flights (id),

    -- Mirrors SeatCount. The record protects the application; this protects the
    -- data against anything writing to the table without going through it.
                             CONSTRAINT chk_seat_blocks_seats_within_booking_limit
                                 CHECK (seats BETWEEN 1 AND 9)
);

-- Both unique indexes are a backstop rather than the mechanism. BlockSeatsService
-- checks for an existing hold after locking the flight, by which point a
-- competing request has already committed or is still waiting, so in practice
-- neither fires. They cover what the lock does not, such as one key arriving for
-- two different flights.
CREATE UNIQUE INDEX uq_seat_blocks_idempotency_key
    ON seat_blocks (idempotency_key);

-- A booking holds seats once. Without this, two requests carrying different
-- idempotency keys for one booking would each take a set of seats.
CREATE UNIQUE INDEX uq_seat_blocks_booking
    ON seat_blocks (booking_id);

-- Releasing a hold and reconciling a flight's seat count both start from the
-- flight.
CREATE INDEX idx_seat_blocks_flight
    ON seat_blocks (flight_id);