-- One database per service, on a single local instance.
-- In production these would be separate instances; sharing one container is a
-- local-development convenience only (see ADR-002).
--
-- This script runs ONLY on first start, when the postgres-data volume is empty.
-- To re-run it: docker compose down -v

CREATE DATABASE airline_flight;
CREATE DATABASE airline_auth;
CREATE DATABASE airline_booking;
CREATE DATABASE airline_payment;
CREATE DATABASE airline_checkin;

-- notification-service holds no state of its own and needs no database.
