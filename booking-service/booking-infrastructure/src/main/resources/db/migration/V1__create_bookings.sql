CREATE TABLE bookings (
    id               UUID           PRIMARY KEY,
    passenger_id     UUID           NOT NULL,
    flight_id        UUID           NOT NULL,
    seat_block_id    UUID           NOT NULL,
    seats            INTEGER        NOT NULL,
    price_amount     NUMERIC(12, 2) NOT NULL,
    price_currency   VARCHAR(3)     NOT NULL,
    total_amount     NUMERIC(12, 2) NOT NULL,
    total_currency   VARCHAR(3)     NOT NULL,
    status           VARCHAR(16)    NOT NULL,
    idempotency_key  VARCHAR(255)   NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL,

    CONSTRAINT chk_bookings_seats_within_booking_limit
        CHECK (seats BETWEEN 1 AND 9),
    CONSTRAINT chk_bookings_price_non_negative
        CHECK (price_amount >= 0),
    CONSTRAINT chk_bookings_total_matches_fare
        CHECK (total_amount = price_amount * seats),
    CONSTRAINT chk_bookings_one_currency
        CHECK (total_currency = price_currency)
);

CREATE UNIQUE INDEX uq_bookings_idempotency_key
    ON bookings (idempotency_key);

CREATE UNIQUE INDEX uq_bookings_seat_block
    ON bookings (seat_block_id);

CREATE INDEX idx_bookings_passenger
    ON bookings (passenger_id, created_at DESC);
