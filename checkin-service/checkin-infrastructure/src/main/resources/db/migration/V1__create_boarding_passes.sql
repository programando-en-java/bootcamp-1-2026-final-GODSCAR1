CREATE TABLE boarding_passes (
    id                  UUID        PRIMARY KEY,
    -- The natural key. A booking is checked in once, and the constraint is what
    -- makes a repeated request return the pass instead of printing a second.
    booking_id          UUID        NOT NULL UNIQUE,
    passenger_id        UUID        NOT NULL,

    -- What the flight said when the pass was printed. Copied on purpose: a pass
    -- is a document, and reprinting it must not depend on flight-service.
    flight_id           UUID        NOT NULL,
    flight_number       VARCHAR(16) NOT NULL,
    origin              VARCHAR(3)  NOT NULL,
    destination         VARCHAR(3)  NOT NULL,
    departure_time      TIMESTAMPTZ NOT NULL,

    boarding_sequence   INTEGER     NOT NULL,
    issued_at           TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_boarding_passes_sequence_positive
        CHECK (boarding_sequence >= 1)
);

CREATE INDEX idx_boarding_passes_flight
    ON boarding_passes (flight_id, boarding_sequence);

-- One row per flight, holding the last place handed out. Read for update, so
-- two passengers checking in at the same moment take turns over this row.
CREATE TABLE boarding_sequences (
    flight_id       UUID    PRIMARY KEY,
    last_sequence   INTEGER NOT NULL,

    CONSTRAINT chk_boarding_sequences_not_negative
        CHECK (last_sequence >= 0)
);
