CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,

    -- BCrypt output is 60 characters and its own salt is inside it, so there is
    -- no second column and no length to guess at.
    password_hash VARCHAR(72)  NOT NULL,

    -- Comma separated rather than a join table. A user has one or two roles out
    -- of three fixed values, and a table to hold that would be read on every
    -- login and never queried any other way.
    roles         VARCHAR(64)  NOT NULL,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_users_roles_not_empty CHECK (length(trim(roles)) > 0)
);

-- Demo accounts, one per role, so the smoke scripts and anyone reading this
-- have somewhere to log in from. The passwords are in scripts/README.md and in
-- nobody's idea of a secret: this system has no sign up, because no story asks
-- for one (ADR-020).
INSERT INTO users (id, email, password_hash, roles) VALUES
    ('11111111-1111-4111-8111-111111111111',
     'passenger@airline.test',
     '$2a$10$rngFwfivHEfHEeFW4Hq0weoQxGM4R9jNETR6L9pDTe5GYhZ3W9r3O',
     'PASSENGER'),
    ('22222222-2222-4222-8222-222222222222',
     'agent@airline.test',
     '$2a$10$a1r53enqPjqaP4tvHmDDaeem/OdUCtalRuIdoW27IS4tJU.zajvb.',
     'AGENT'),
    ('33333333-3333-4333-8333-333333333333',
     'admin@airline.test',
     '$2a$10$M4IKO9xsP5bRfoKRnosb1OBd6h2GWlmkTaDWhveQM3CJ5aJOSW.I2',
     'ADMIN');
