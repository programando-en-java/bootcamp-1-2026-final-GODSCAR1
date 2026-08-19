-- What this service has already acted on. Delivery is at-least-once (ADR-001),
-- so the same message arrives more than once and the primary key is what makes
-- the second arrival a no-op rather than a second booking confirmed.
CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Rows are never removed today. A retention job is the missing piece, and the
-- table grows with every message this service consumes.
