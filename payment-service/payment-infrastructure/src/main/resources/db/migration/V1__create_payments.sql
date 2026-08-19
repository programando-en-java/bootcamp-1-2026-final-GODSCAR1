CREATE TABLE payments (
    id                      UUID           PRIMARY KEY,
    booking_id              UUID           NOT NULL,
    amount                  NUMERIC(12, 2) NOT NULL,
    currency                VARCHAR(3)     NOT NULL,
    status                  VARCHAR(16)    NOT NULL,
    -- VARCHAR, not CHAR: @Column(length = 4) maps to varchar, and Hibernate
    -- validates the type as well as the width. The check below is what makes
    -- these four digits.
    card_last_four_digits   VARCHAR(4)     NOT NULL,
    processed_at            TIMESTAMPTZ    NOT NULL,

    CONSTRAINT chk_payments_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_payments_last_four_are_digits
        CHECK (card_last_four_digits ~ '^[0-9]{4}$')
);

-- Many attempts, one charge. A booking can be refused any number of times and
-- paid once.
CREATE UNIQUE INDEX uq_payments_settled_booking
    ON payments (booking_id)
    WHERE status = 'SUCCEEDED';

CREATE INDEX idx_payments_booking ON payments (booking_id, processed_at DESC);
