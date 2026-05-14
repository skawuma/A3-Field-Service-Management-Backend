CREATE TABLE work_order_events (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    message VARCHAR(500) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    actor VARCHAR(150)
);
