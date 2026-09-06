CREATE TABLE shows (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    organizer_id UUID NOT NULL,
    venue_id UUID NOT NULL,
    hall_id UUID NOT NULL,
    language VARCHAR(50) NOT NULL DEFAULT 'en',
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_shows_status CHECK (status IN ('SCHEDULED', 'ACTIVE', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_shows_dates CHECK (end_at > start_at)
);

CREATE TABLE show_prices (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    amount_cents INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    CONSTRAINT fk_show_prices_show FOREIGN KEY (show_id) REFERENCES shows (id) ON DELETE CASCADE,
    CONSTRAINT uk_show_prices UNIQUE (show_id, seat_type),
    CONSTRAINT chk_show_prices_type CHECK (seat_type IN ('REGULAR', 'PREMIUM', 'VIP')),
    CONSTRAINT chk_show_prices_amount CHECK (amount_cents >= 0)
);

CREATE INDEX idx_shows_event ON shows (event_id);
CREATE INDEX idx_shows_organizer ON shows (organizer_id);
CREATE INDEX idx_shows_venue_hall ON shows (venue_id, hall_id);
CREATE INDEX idx_shows_status ON shows (status);
CREATE INDEX idx_shows_start ON shows (start_at);
