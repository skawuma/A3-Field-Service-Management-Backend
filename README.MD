# A3 Field Service Management Backend

## Plain-English Overview

The backend is the rule engine of the A3 FSM platform.

It is responsible for:

- deciding what a user is allowed to do
- enforcing the work order lifecycle
- storing work orders, reports, attachments, and events
- powering the dashboard with operational and analytical data
- protecting data integrity even if the UI makes a bad request

In short: the frontend shows actions, but the backend is the final source of truth.

## How To Read This With The Root Diagrams

In the combined workspace, the root README is the visual entry point for the whole platform. Start there if you want the clearest picture of:

- the current-state architecture
- the future-state architecture direction
- the realtime assignment, completion, and SLA event flow

This backend README then zooms in on the server-side responsibilities that make those visuals real.

## Backend Highlights For Reviewers

This backend is a strong project talking point because it demonstrates more than endpoint creation:

- workflow enforcement lives on the server, not just in the UI
- timeline events make the system auditable and easier to reason about
- dashboard endpoints return frontend-friendly data instead of leaking raw database structure
- the design already hints at future service boundaries without over-engineering the current system

## What This Service Does

### Core Business Responsibilities

- authenticate users with JWT-based security
- create, update, assign, and track work orders
- allow technicians to start, document, and complete work
- enforce reopen and return-to-open rules
- capture timeline events for auditability
- generate dashboard summary, analytics, SLA, and workload data

### Why This Matters

For a field service platform, backend enforcement is critical because the user interface alone cannot guarantee process integrity.

Examples:

- a work order cannot be completed unless it is `IN_PROGRESS`
- a completed job cannot keep accepting normal technician edits
- only the assigned technician should be able to start work
- only admin or dispatch roles should be able to reopen completed work

## Service-Level Architecture Diagram

```mermaid
flowchart TB
    C[Controllers] --> S[Services]
    S --> R[Repositories]
    R --> DB[(PostgreSQL)]

    S --> E[Event Logging Services]
    S --> F[File and Attachment Services]
    S --> D[Dashboard Aggregation Services]
    C --> SEC[JWT Security Filter Chain]
```

This is the backend slice of the broader current-state architecture shown in the root README.

## Main Architectural Layers

### Controller Layer

Receives HTTP requests and exposes REST endpoints for:

- authentication
- work orders
- attachments
- dashboard and analytics
- technician-related workflows

### Service Layer

Contains the real business logic:

- validates transitions
- checks role permissions
- coordinates report and timeline changes
- shapes dashboard data into frontend-friendly responses

### Repository Layer

Handles persistence using Spring Data JPA and PostgreSQL for:

- work orders
- users and technicians
- work order events
- structured completion reports
- attachments and related metadata

## Domain Flow Diagram

```mermaid
flowchart LR
    WO[Work Order] --> EVT[Work Order Events]
    WO --> ATT[Attachments]
    WO --> REP[Completion Report]
    WO --> TECH[Assigned Technician]
    USER[Authenticated User] --> WO
    DASH[Dashboard Service] --> WO
    DASH --> EVT
```

## Work Order Lifecycle Rules

The backend enforces the lifecycle so the application behaves consistently for every user and every client.

```mermaid
stateDiagram-v2
    [*] --> OPEN
    OPEN --> ASSIGNED: assign technician
    ASSIGNED --> IN_PROGRESS: start work
    OPEN --> IN_PROGRESS: start when permitted
    IN_PROGRESS --> COMPLETED: complete with signature/report
    ASSIGNED --> OPEN: return to open
    COMPLETED --> OPEN: reopen by admin/dispatch
    OPEN --> CANCELLED: cancel
    ASSIGNED --> CANCELLED: cancel
```

### Key Enforcement Rules

- `START` is only valid from `OPEN` or `ASSIGNED`
- `COMPLETE` is only valid from `IN_PROGRESS`
- `REOPEN` is only valid from `COMPLETED`
- `RETURN TO OPEN` is not allowed from `IN_PROGRESS`
- `RETURN TO OPEN` is not allowed from `COMPLETED`
- completion requires signature support
- duplicate completion report submission is blocked
- completed work orders are treated as read-only for normal technician edits

## Request Workflow Diagram

```mermaid
sequenceDiagram
    participant Client as Angular Client
    participant Security as JWT Security
    participant Controller as REST Controller
    participant Service as WorkOrderService
    participant Event as WorkOrderEventService
    participant DB as PostgreSQL

    Client->>Security: Request with access token
    Security->>Controller: Authenticated user context
    Controller->>Service: start / complete / reopen request
    Service->>Service: Validate role and state transition
    Service->>DB: Save work order changes
    Service->>Event: Log timeline event
    Event->>DB: Persist event record
    DB-->>Controller: Saved data
    Controller-->>Client: Updated response
```

## Event Timeline Philosophy

The backend does not just save the latest state. It also records the story of what happened.

Important event types include:

- `CREATED`
- `STATUS_CHANGED`
- `ASSIGNED_TECHNICIAN`
- `UNASSIGNED_TECHNICIAN`
- `STARTED`
- `COMPLETED`
- `REOPENED`
- `NOTE_ADDED`
- `ATTACHMENT_ADDED`

This makes the system easier to audit, support, and eventually analyze for operational trends.

## Dashboard Responsibilities

The backend now powers three dashboard layers:

### Operational Summary

- KPI counts
- completed today
- high-priority open work
- recent activity messages shaped for the UI

### Analytics

- work orders by status
- work orders by priority
- completion trend

### SLA And Workload

- overdue work orders
- due-today work orders
- technician workload totals
- overdue and due-today workload pressure

```mermaid
flowchart LR
    WO[Work Orders] --> DS[DashboardService]
    EVT[Work Order Events] --> DS
    TECH[Technicians] --> DS
    DS --> SUM[Summary DTO]
    DS --> ACT[Recent Activity DTO]
    DS --> ANA[Analytics DTO]
    DS --> SLA[SLA DTO]
    DS --> WL[Technician Workload DTO]
```

## Sprint 8 Realtime Layer

Sprint 8 adds a realtime event layer on top of the existing modular monolith.

The backend now supports:

- Spring WebSocket/STOMP over `/ws`
- dashboard broadcasts on `/topic/dashboard`
- operational alerts on `/topic/alerts`
- technician-specific notifications on `/user/queue/notifications`
- transaction-safe event publishing after database commits
- enriched event metadata for UI updates without a refresh

```mermaid
flowchart LR
    WS[WorkOrderService] --> PUB[RealtimeEventPublisher]
    DS[DashboardService] --> PUB
    SLA[SlaRealtimeMonitor] --> PUB

    PUB --> DTOPIC[/topic/dashboard/]
    PUB --> ATOPIC[/topic/alerts/]
    PUB --> UQUEUE[/user/queue/notifications/]

    DTOPIC --> DASH[Dashboard and work order screens]
    ATOPIC --> ALERTS[SLA alert toasts]
    UQUEUE --> TECH[Assigned technician toast]
```

### Realtime Workflow Events

The realtime publisher emits events for:

- work order created
- technician assigned
- work order started
- completion report submitted
- work order completed
- work order reopened
- work order returned to open
- SLA breached

Each event carries enough metadata for the frontend to update visible rows, detail pages, dashboard cards, recent activity, workload pressure, and notification text without polling.

### SLA Scanner

SLA intelligence has two entry points:

- dashboard SLA reads can publish newly discovered overdue work orders
- `SlaRealtimeMonitor` scans overdue active work orders on a schedule

Duplicate alert prevention is centralized in `RealtimeEventPublisher` with an in-memory set of already published overdue work order IDs. The scheduled scanner sends a full overdue snapshot so resolved jobs can leave the de-duplication set.

```mermaid
sequenceDiagram
    participant Scheduler as SlaRealtimeMonitor
    participant Repo as WorkOrderRepository
    participant Pub as RealtimeEventPublisher
    participant Broker as STOMP Broker
    participant UI as Angular Clients

    Scheduler->>Repo: Find active work orders past scheduled date
    Repo-->>Scheduler: Overdue work orders
    Scheduler->>Pub: publishNewSlaBreaches(..., full snapshot)
    Pub->>Pub: Skip already published overdue IDs
    Pub->>Broker: /topic/dashboard and /topic/alerts
    Broker-->>UI: SLA card refresh and alert toast
```

## How The Root Visuals Map To Backend Responsibilities

The visual diagrams in the root README connect to this backend in three simple ways:

- current-state architecture: this backend owns the business rules, persistence, APIs, and server-side realtime behavior behind the Angular client
- future-state architecture: the services shown there reflect the clean boundaries already forming here around auth, work orders, SLA logic, dashboard aggregation, and notifications
- realtime business flow: assignment, completion, and SLA events all rely on backend validation, event logging, data updates, and broadcasting

## What To Emphasize In A Demo

If you are discussing the backend in a review or interview, the strongest points are:

- illegal workflow transitions are blocked at the service layer
- completion reporting is structured, not just free text
- dashboard intelligence is aggregated intentionally for UI consumption
- the current monolith is organized in a way that could evolve toward clearer service separation later

## Security Model

### Authentication

- JWT access tokens protect API requests
- refresh tokens support longer signed-in sessions
- role checks determine which endpoints and actions are allowed

### Role Intent

- `ADMIN`: highest level oversight and lifecycle recovery actions
- `DISPATCH`: assignment and operational coordination
- `TECHNICIAN`: execution, notes, completion, and field updates

## Role-Based Behavior In Practice

### Admin / Dispatch

- can reopen completed work orders
- can view broader dashboard and timeline visibility
- can manage assignments and operational recovery

### Technician

- can start assigned work
- can return eligible work to `OPEN`
- can submit notes, attachments, and structured completion
- cannot use admin-only recovery actions

## Sprint 10 Demo Profile

The public portfolio demo is isolated behind the `demo` Spring profile and an explicit data flag:

```bash
SPRING_PROFILES_ACTIVE=prod,demo
DEMO_DATA_ENABLED=true
```

When both are active, the backend idempotently creates three public role accounts, four fictional technicians, eight realistic work orders, recent timeline events, overdue and unassigned work, and two structured completion reports. Demo passwords are processed by the existing BCrypt encoder before storage.

The seed does not run in ordinary production. Demo mode also blocks permanent technician deletion, while self-registration and admin bootstrap remain disabled.

## Local Development

### Run With Maven

```bash
./mvnw spring-boot:run
```

Typical local API base:

- `http://localhost:8080`

### Docker-Aware Notes

- use the root `docker-compose.yml` as the single container entry point for the full platform
- the backend no longer keeps a separate backend-only compose file, which avoids config drift
- uploads are mounted so files persist outside the container
- demo bootstrap support exists for container-first development
- a local database import helper is available when you want Docker to use existing local data

## API Direction

Representative endpoint groups include:

- `/api/auth/*`
- `/api/workorders/*`
- `/api/dashboard/summary`
- `/api/dashboard/recent-activity`
- `/api/dashboard/analytics`
- `/api/dashboard/sla`
- `/api/dashboard/technician-workload`

## Design Principles

This backend is designed around a few practical principles:

- business rules belong on the server
- dashboard endpoints should return UI-friendly data, not raw internal tables
- operational history should be explainable from timeline events
- new reporting features should build on clean DTOs rather than leaking persistence details

## Near-Term Evolution

As the platform moves beyond Sprint 8, the backend is well positioned for:

- Kafka-backed event publishing behind the current `RealtimeEventPublisher` contract
- richer persisted notification and alert history
- technician-specific recent activity feeds backed by durable subscriptions
- deeper workload balancing and assignment intelligence
- future microservice extraction around auth, work orders, SLA, dashboard, and notifications

## Related Documentation

- [Root README](../README.MD)
- [Frontend README](../A3%20Field%20Service%20Management%20Frontend/a3-fsm-frontend/README.md)

## Closing Summary

The backend gives the platform its operational discipline.

It is where:

- permissions are checked
- lifecycle rules are enforced
- evidence and reporting are stored
- dashboard intelligence is assembled

That makes it the part of the system that keeps the application trustworthy as the UI becomes more powerful and more real-time.
