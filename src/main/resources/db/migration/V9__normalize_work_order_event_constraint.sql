-- Older installations used three different names for the same event-type
-- constraint. Drop every known legacy name so new Sprint 11 event types are
-- not rejected by a stale constraint left alongside the canonical one.
ALTER TABLE work_order_events DROP CONSTRAINT IF EXISTS chk_work_order_events_event_type;
ALTER TABLE work_order_events DROP CONSTRAINT IF EXISTS chk_work_order_events_type;
ALTER TABLE work_order_events DROP CONSTRAINT IF EXISTS work_order_events_event_type_check;

ALTER TABLE work_order_events ADD CONSTRAINT work_order_events_event_type_check CHECK (
    event_type IN (
        'CREATED', 'UPDATED_DETAILS', 'STATUS_CHANGED', 'PRIORITY_CHANGED',
        'ASSIGNED_TECHNICIAN', 'UNASSIGNED_TECHNICIAN', 'STARTED',
        'TRAVEL_STARTED', 'ARRIVED_ONSITE', 'WORK_STARTED', 'SLA_NEAR_BREACH',
        'SLA_BREACHED', 'SLA_MET', 'SCHEDULED_DATE_CHANGED', 'NOTE_ADDED',
        'ATTACHMENT_ADDED', 'REOPENED', 'COMPLETED'
    )
);
