CREATE TABLE show_seats (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    state VARCHAR(20) NOT NULL,
    lock_id UUID,
    lock_owner_id UUID,
    lock_expires_at TIMESTAMPTZ,
    booking_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_show_seats UNIQUE (show_id, seat_id),
    CONSTRAINT chk_show_seats_state CHECK (state IN ('AVAILABLE', 'LOCKED', 'BOOKED')),
    CONSTRAINT chk_show_seats_type CHECK (seat_type IN ('REGULAR', 'PREMIUM', 'VIP'))
);

CREATE TABLE inventory_commands (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    command_type VARCHAR(40) NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_show_seats_show_state ON show_seats (show_id, state);
CREATE INDEX idx_show_seats_lock ON show_seats (lock_id);
CREATE INDEX idx_show_seats_expires ON show_seats (lock_expires_at);
