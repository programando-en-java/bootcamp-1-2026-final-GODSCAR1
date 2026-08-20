-- What a passenger was told, and whether it got out. There is no read endpoint:
-- this is the record of a send, not an inbox (ADR-017).
CREATE TABLE notifications (
    id           UUID          PRIMARY KEY,
    passenger_id UUID          NOT NULL,
    booking_id   UUID          NOT NULL,
    type         VARCHAR(32)   NOT NULL,
    subject      VARCHAR(128)  NOT NULL,
    body         VARCHAR(1024) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,

    -- Null until the channel has taken it. A row that stays null is a
    -- notification nobody received, which is the point of recording it apart
    -- from created_at.
    sent_at      TIMESTAMPTZ
);

CREATE INDEX idx_notifications_passenger
    ON notifications (passenger_id, created_at DESC);

-- Partial, so it stays the size of whatever went wrong rather than the size of
-- the table.
CREATE INDEX idx_notifications_unsent
    ON notifications (created_at)
    WHERE sent_at IS NULL;

-- What this service has already acted on. Delivery is at-least-once (ADR-001),
-- and the primary key is what makes a redelivered message a no-op rather than a
-- second notification to someone who already read the first (ADR-014).
CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
