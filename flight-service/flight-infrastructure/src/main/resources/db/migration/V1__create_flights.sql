CREATE TABLE flights (
                         id              UUID           PRIMARY KEY,
                         flight_number   VARCHAR(16)    NOT NULL,
                         origin          VARCHAR(3)     NOT NULL,
                         destination     VARCHAR(3)     NOT NULL,
                         departure_time  TIMESTAMPTZ    NOT NULL,
                         arrival_time    TIMESTAMPTZ    NOT NULL,
                         total_seats     INTEGER        NOT NULL,
                         available_seats INTEGER        NOT NULL,
                         price_amount    NUMERIC(12, 2) NOT NULL,
                         price_currency  VARCHAR(3)     NOT NULL,

    -- These mirror the invariants in the domain records. Duplicated on purpose:
    -- the records protect the application, the constraints protect the data
    -- against anything that writes to this table without going through them.
                         CONSTRAINT chk_flights_seats_within_capacity
                             CHECK (available_seats BETWEEN 0 AND total_seats),
                         CONSTRAINT chk_flights_capacity_positive
                             CHECK (total_seats > 0),
                         CONSTRAINT chk_flights_arrival_after_departure
                             CHECK (arrival_time > departure_time),
                         CONSTRAINT chk_flights_distinct_endpoints
                             CHECK (origin <> destination),
                         CONSTRAINT chk_flights_price_non_negative
                             CHECK (price_amount >= 0)
);

-- Supports the filtered search: equality on the route, range on the departure.
CREATE INDEX idx_flights_route_departure
    ON flights (origin, destination, departure_time);

-- An unfiltered search still orders the whole catalogue by departure.
CREATE INDEX idx_flights_departure
    ON flights (departure_time);

-- The same physical flight cannot be scheduled twice.
CREATE UNIQUE INDEX uq_flights_number_departure
    ON flights (flight_number, departure_time);