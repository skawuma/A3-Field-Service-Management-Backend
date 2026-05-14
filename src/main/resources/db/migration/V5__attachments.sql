CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT,
    filename VARCHAR(255),
    url VARCHAR(255),
    content_type VARCHAR(255),
    size_bytes BIGINT NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMPTZ,
    uploaded_by VARCHAR(255)
);
