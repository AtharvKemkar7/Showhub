CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    amount_cents INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(40) NOT NULL DEFAULT 'MOCK',
    provider_ref VARCHAR(100),
    failure_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_payments_status CHECK (status IN ('INITIATED', 'SUCCEEDED', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payments_amount CHECK (amount_cents >= 0)
);

CREATE TABLE payment_commands (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    command_type VARCHAR(40) NOT NULL,
    payment_id UUID,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_payments_booking_success ON payments (booking_id) WHERE status IN ('INITIATED', 'SUCCEEDED');
CREATE INDEX idx_payments_customer ON payments (customer_id);
CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_provider_ref ON payments (provider_ref);
