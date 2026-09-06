CREATE TABLE search_events (
    event_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    city VARCHAR(100),
    category_name VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    organizer_id UUID,
    start_at TIMESTAMPTZ,
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_events_status ON search_events (status);
CREATE INDEX idx_search_events_city ON search_events (city);
CREATE INDEX idx_search_events_title ON search_events (lower(title));
CREATE INDEX idx_search_events_start ON search_events (start_at);
