CREATE TABLE timesheets (
    id BIGSERIAL PRIMARY KEY,
    technician_id BIGINT NOT NULL,
    technician_name VARCHAR(255) NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    technician_signature_text VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_timesheets_technician_week UNIQUE (technician_id, week_start_date),
    CONSTRAINT chk_timesheets_status CHECK (
        status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'EXPORTED')
    ),
    CONSTRAINT chk_timesheets_week CHECK (week_end_date = week_start_date + 6),
    CONSTRAINT fk_timesheets_technician FOREIGN KEY (technician_id)
        REFERENCES technicians (id) ON DELETE RESTRICT
);

CREATE TABLE timesheet_entries (
    id BIGSERIAL PRIMARY KEY,
    timesheet_id BIGINT NOT NULL,
    work_order_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    site_address VARCHAR(500),
    city VARCHAR(120),
    state VARCHAR(80),
    zip VARCHAR(20),
    miles NUMERIC(8, 2),
    onsite_start_time TIME,
    break_start_time TIME,
    break_end_time TIME,
    offsite_end_time TIME,
    comments VARCHAR(3000),
    auto_generated BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_timesheet_entries_work_order UNIQUE (work_order_id),
    CONSTRAINT chk_timesheet_entries_miles CHECK (miles IS NULL OR miles >= 0),
    CONSTRAINT fk_timesheet_entries_timesheet FOREIGN KEY (timesheet_id)
        REFERENCES timesheets (id) ON DELETE CASCADE,
    CONSTRAINT fk_timesheet_entries_work_order FOREIGN KEY (work_order_id)
        REFERENCES work_orders (id) ON DELETE RESTRICT
);

CREATE INDEX idx_timesheets_week_start ON timesheets (week_start_date);
CREATE INDEX idx_timesheets_status ON timesheets (status);
CREATE INDEX idx_timesheets_technician ON timesheets (technician_id);
CREATE INDEX idx_timesheet_entries_timesheet ON timesheet_entries (timesheet_id);
CREATE INDEX idx_timesheet_entries_work_date ON timesheet_entries (work_date);
