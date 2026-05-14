ALTER TABLE users
    ADD CONSTRAINT chk_users_role
    CHECK (role IN ('ADMIN', 'DISPATCH', 'TECH'));

ALTER TABLE technicians
    ADD CONSTRAINT chk_technicians_status
    CHECK (status IS NULL OR status IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE technicians
    ADD CONSTRAINT fk_technicians_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE SET NULL;

ALTER TABLE work_orders
    ADD CONSTRAINT chk_work_orders_status
    CHECK (status IS NULL OR status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

ALTER TABLE work_orders
    ADD CONSTRAINT fk_work_orders_assigned_technician
    FOREIGN KEY (assigned_tech_id) REFERENCES technicians (id)
    ON DELETE SET NULL;

ALTER TABLE work_order_completions
    ADD CONSTRAINT chk_work_order_completions_replacement_needed
    CHECK (replacement_needed IN ('YES', 'NO', 'PROBABLE'));

ALTER TABLE work_order_completions
    ADD CONSTRAINT fk_work_order_completions_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_orders (id)
    ON DELETE CASCADE;

ALTER TABLE work_order_completions
    ADD CONSTRAINT fk_work_order_completions_completed_by_user
    FOREIGN KEY (completed_by_user_id) REFERENCES users (id)
    ON DELETE RESTRICT;

ALTER TABLE attachments
    ADD CONSTRAINT fk_attachments_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_orders (id)
    ON DELETE CASCADE;

ALTER TABLE work_order_events
    ADD CONSTRAINT chk_work_order_events_event_type
    CHECK (event_type IN (
        'CREATED',
        'UPDATED_DETAILS',
        'STATUS_CHANGED',
        'PRIORITY_CHANGED',
        'ASSIGNED_TECHNICIAN',
        'UNASSIGNED_TECHNICIAN',
        'STARTED',
        'SCHEDULED_DATE_CHANGED',
        'NOTE_ADDED',
        'ATTACHMENT_ADDED',
        'REOPENED',
        'COMPLETED'
    ));

ALTER TABLE work_order_events
    ADD CONSTRAINT fk_work_order_events_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_orders (id)
    ON DELETE CASCADE;

CREATE INDEX idx_technicians_status ON technicians (status);
CREATE INDEX idx_technicians_user_id ON technicians (user_id);

CREATE INDEX idx_work_orders_status ON work_orders (status);
CREATE INDEX idx_work_orders_assigned_tech_id ON work_orders (assigned_tech_id);
CREATE INDEX idx_work_orders_scheduled_date ON work_orders (scheduled_date);
CREATE INDEX idx_work_orders_priority ON work_orders (priority);
CREATE INDEX idx_work_orders_created_at ON work_orders (created_at);

CREATE INDEX idx_work_order_completions_completed_by_user_id
    ON work_order_completions (completed_by_user_id);

CREATE INDEX idx_attachments_work_order_id ON attachments (work_order_id);
CREATE INDEX idx_attachments_uploaded_at ON attachments (uploaded_at);

CREATE INDEX idx_work_order_events_work_order_id ON work_order_events (work_order_id);
CREATE INDEX idx_work_order_events_created_at ON work_order_events (created_at);
CREATE INDEX idx_work_order_events_event_type ON work_order_events (event_type);
