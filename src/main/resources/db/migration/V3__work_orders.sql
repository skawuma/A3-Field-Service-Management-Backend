CREATE TABLE work_orders (
    id BIGSERIAL PRIMARY KEY,
    client_name VARCHAR(255),
    address VARCHAR(255),
    description VARCHAR(2000),
    status VARCHAR(255),
    assigned_tech_id BIGINT,
    scheduled_date DATE,
    priority VARCHAR(255),
    signature_url VARCHAR(1000),
    completion_notes VARCHAR(2000),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
