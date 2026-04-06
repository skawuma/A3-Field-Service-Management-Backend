-- Sprint 7 SLA verification seed data
-- Re-runnable: removes prior seed rows by client name prefix before inserting.

delete from work_orders
where client_name like 'SLA Seed %';

insert into work_orders (
    client_name,
    address,
    description,
    status,
    assigned_tech_id,
    scheduled_date,
    priority,
    signature_url,
    completion_notes,
    completed_at,
    created_at
) values
(
    'SLA Seed Yesterday Open',
    '100 SLA Avenue',
    'Scheduled yesterday and still open to verify overdue logic.',
    'OPEN',
    null,
    current_date - 1,
    'MEDIUM',
    null,
    null,
    null,
    now()
),
(
    'SLA Seed Yesterday In Progress',
    '101 SLA Avenue',
    'Scheduled yesterday and currently in progress to verify overdue logic.',
    'IN_PROGRESS',
    1,
    current_date - 1,
    'HIGH',
    null,
    null,
    null,
    now()
),
(
    'SLA Seed Yesterday Completed',
    '102 SLA Avenue',
    'Scheduled yesterday and completed so it should not count as overdue.',
    'COMPLETED',
    1,
    current_date - 1,
    'LOW',
    'signatures/sla-seed-yesterday-completed.png',
    'Completed for SLA exclusion verification.',
    now() - interval '1 day',
    now()
),
(
    'SLA Seed Today Open',
    '103 SLA Avenue',
    'Scheduled today and open to verify due today logic.',
    'OPEN',
    null,
    current_date,
    'HIGH',
    null,
    null,
    null,
    now()
),
(
    'SLA Seed Today In Progress',
    '104 SLA Avenue',
    'Scheduled today and in progress to verify due today logic.',
    'IN_PROGRESS',
    1,
    current_date,
    'CRITICAL',
    null,
    null,
    null,
    now()
),
(
    'SLA Seed Today Completed',
    '105 SLA Avenue',
    'Scheduled today and completed so it should not count as due today.',
    'COMPLETED',
    1,
    current_date,
    'MEDIUM',
    'signatures/sla-seed-today-completed.png',
    'Completed for SLA exclusion verification.',
    now(),
    now()
);
