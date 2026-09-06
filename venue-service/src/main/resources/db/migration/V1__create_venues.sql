CREATE TABLE venues (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(300) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_venues_name_city UNIQUE (name, city)
);

CREATE TABLE halls (
    id UUID PRIMARY KEY,
    venue_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    capacity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_halls_venue FOREIGN KEY (venue_id) REFERENCES venues (id) ON DELETE CASCADE,
    CONSTRAINT uk_halls_venue_name UNIQUE (venue_id, name),
    CONSTRAINT chk_halls_capacity CHECK (capacity > 0)
);

CREATE TABLE seats (
    id UUID PRIMARY KEY,
    hall_id UUID NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_seats_hall FOREIGN KEY (hall_id) REFERENCES halls (id) ON DELETE CASCADE,
    CONSTRAINT uk_seats_hall_row_number UNIQUE (hall_id, row_label, seat_number),
    CONSTRAINT chk_seats_type CHECK (seat_type IN ('REGULAR', 'PREMIUM', 'VIP')),
    CONSTRAINT chk_seats_number CHECK (seat_number > 0)
);

CREATE INDEX idx_venues_city ON venues (city);
CREATE INDEX idx_venues_active ON venues (active);
CREATE INDEX idx_halls_venue ON halls (venue_id);
CREATE INDEX idx_seats_hall ON seats (hall_id);
CREATE INDEX idx_seats_enabled ON seats (hall_id, enabled);
