CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    show_id UUID NOT NULL,
    event_id UUID NOT NULL,
    organizer_id UUID NOT NULL,
    venue_id UUID NOT NULL,
    hall_id UUID NOT NULL,
    lock_id UUID,
    lock_expires_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    total_cents INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    payment_id UUID,
    cancel_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_bookings_total CHECK (total_cents >= 0)
);

CREATE TABLE booking_items (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    seat_type VARCHAR(20) NOT NULL,
    amount_cents INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    CONSTRAINT fk_booking_items_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT uk_booking_items UNIQUE (booking_id, seat_id),
    CONSTRAINT chk_booking_items_amount CHECK (amount_cents >= 0)
);

CREATE TABLE booking_commands (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    command_type VARCHAR(40) NOT NULL,
    booking_id UUID,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_customer ON bookings (customer_id);
CREATE INDEX idx_bookings_show ON bookings (show_id);
CREATE INDEX idx_bookings_organizer ON bookings (organizer_id);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_bookings_lock ON bookings (lock_id);
