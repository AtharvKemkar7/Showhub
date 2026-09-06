CREATE TABLE event_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_event_categories_name UNIQUE (name),
    CONSTRAINT uk_event_categories_slug UNIQUE (slug)
);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    organizer_id UUID NOT NULL,
    category_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    venue_name VARCHAR(200) NOT NULL,
    venue_address VARCHAR(300) NOT NULL,
    city VARCHAR(100) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_events_category FOREIGN KEY (category_id) REFERENCES event_categories (id),
    CONSTRAINT chk_events_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_events_dates CHECK (end_at > start_at)
);

CREATE INDEX idx_events_organizer ON events (organizer_id);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_category ON events (category_id);
CREATE INDEX idx_events_city ON events (city);
CREATE INDEX idx_events_start_at ON events (start_at);
CREATE INDEX idx_events_title ON events (title);

INSERT INTO event_categories (id, name, slug, description)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Music', 'music', 'Concerts and live music'),
    ('22222222-2222-2222-2222-222222222222', 'Sports', 'sports', 'Sporting events'),
    ('33333333-3333-3333-3333-333333333333', 'Theatre', 'theatre', 'Plays and performing arts'),
    ('44444444-4444-4444-4444-444444444444', 'Conference', 'conference', 'Conferences and talks'),
    ('55555555-5555-5555-5555-555555555555', 'Festival', 'festival', 'Festivals and community events');
