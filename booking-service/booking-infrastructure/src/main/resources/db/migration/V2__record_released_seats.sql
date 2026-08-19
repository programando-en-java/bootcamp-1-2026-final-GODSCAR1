-- When the seats a failed booking held were given back. Separate from the
-- status because the status is what a passenger sees, and this is the saga's
-- own bookkeeping: a booking can be FAILED while its seats are still held,
-- which is exactly the window the compensation closes.
ALTER TABLE bookings
    ADD COLUMN seats_released_at TIMESTAMPTZ;

-- Only a failed booking can have given its seats back.
ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_only_failed_release_seats
        CHECK (seats_released_at IS NULL OR status = 'FAILED');

-- What the sweep that retries the compensation would ask for.
CREATE INDEX idx_bookings_awaiting_release
    ON bookings (created_at)
    WHERE status = 'FAILED' AND seats_released_at IS NULL;
