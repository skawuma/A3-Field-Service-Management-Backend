ALTER TABLE work_orders
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS en_route_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS arrived_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS work_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS sla_clock_started_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS sla_due_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS sla_breached BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sla_duration_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS actual_completion_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS breach_minutes INTEGER;

UPDATE work_orders
SET assigned_at = created_at
WHERE assigned_tech_id IS NOT NULL AND assigned_at IS NULL;

UPDATE work_orders
SET work_started_at = created_at,
    sla_clock_started_at = created_at,
    sla_duration_minutes = COALESCE(sla_duration_minutes, 240),
    sla_due_at = COALESCE(sla_due_at, created_at + INTERVAL '240 minutes')
WHERE status = 'IN_PROGRESS';

UPDATE work_orders
SET actual_completion_minutes = GREATEST(0, FLOOR(EXTRACT(EPOCH FROM (completed_at - sla_clock_started_at)) / 60)::INTEGER),
    breach_minutes = GREATEST(0, FLOOR(EXTRACT(EPOCH FROM (completed_at - sla_due_at)) / 60)::INTEGER),
    sla_breached = completed_at > sla_due_at
WHERE status = 'COMPLETED'
  AND completed_at IS NOT NULL
  AND sla_clock_started_at IS NOT NULL
  AND sla_due_at IS NOT NULL;

ALTER TABLE work_orders DROP CONSTRAINT IF EXISTS chk_work_orders_status;
ALTER TABLE work_orders ADD CONSTRAINT chk_work_orders_status CHECK (
    status IS NULL OR status IN (
        'OPEN', 'ASSIGNED', 'EN_ROUTE', 'ARRIVED', 'WORK_STARTED',
        'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    )
);

ALTER TABLE work_orders ADD CONSTRAINT chk_work_orders_sla_duration
    CHECK (sla_duration_minutes IS NULL OR sla_duration_minutes > 0);

ALTER TABLE work_orders ADD CONSTRAINT chk_work_orders_sla_minutes
    CHECK ((actual_completion_minutes IS NULL OR actual_completion_minutes >= 0)
       AND (breach_minutes IS NULL OR breach_minutes >= 0));

CREATE INDEX IF NOT EXISTS idx_work_orders_sla_due_active
    ON work_orders (sla_due_at, status)
    WHERE sla_due_at IS NOT NULL AND status NOT IN ('COMPLETED', 'CANCELLED');

CREATE INDEX IF NOT EXISTS idx_work_orders_assigned_at ON work_orders (assigned_at);
CREATE INDEX IF NOT EXISTS idx_work_orders_completed_sla ON work_orders (completed_at, sla_breached);

ALTER TABLE work_order_events DROP CONSTRAINT IF EXISTS chk_work_order_events_type;
ALTER TABLE work_order_events ADD CONSTRAINT chk_work_order_events_type CHECK (
    event_type IN (
        'CREATED', 'UPDATED_DETAILS', 'STATUS_CHANGED', 'PRIORITY_CHANGED',
        'ASSIGNED_TECHNICIAN', 'UNASSIGNED_TECHNICIAN', 'STARTED',
        'TRAVEL_STARTED', 'ARRIVED_ONSITE', 'WORK_STARTED', 'SLA_NEAR_BREACH',
        'SLA_BREACHED', 'SLA_MET', 'SCHEDULED_DATE_CHANGED', 'NOTE_ADDED',
        'ATTACHMENT_ADDED', 'REOPENED', 'COMPLETED'
    )
);
