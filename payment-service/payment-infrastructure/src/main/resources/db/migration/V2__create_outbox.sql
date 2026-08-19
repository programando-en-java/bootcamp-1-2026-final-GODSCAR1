CREATE TABLE outbox (
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(64)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

-- Partial, so it stays the size of the backlog rather than the size of the
-- table. The relay only ever asks for rows that have not gone out.
CREATE INDEX idx_outbox_pending
    ON outbox (created_at)
    WHERE published_at IS NULL;
