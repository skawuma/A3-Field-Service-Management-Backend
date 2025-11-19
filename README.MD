# A3 Field Service Management Platform

> **A3 Solutions – Field Service Management App**  
> Modern web platform to manage work orders, technicians, scheduling, site visits, and post-mortem reports for A3 Solutions field technicians.

---

## 1. Overview

The **A3 Field Service Management App** is a full-stack web platform designed to streamline day-to-day operations for A3 Solutions:

- Create, assign, and track **work orders** and **site visits**
- Record **onsite/offsite times**, **hardware tags (FA tags)**, and **troubleshooting logs**
- Upload **photos and attachments** from the field
- Generate **post-mortem reports** for clients (e.g., Santander, RBC, CNA, etc.)
- Provide **role-based access** for admins, dispatchers, and technicians

The system is built using a **microservice architecture** with a **SPA frontend** and is designed for **scalability, resilience, and clear auditability**.

---

## 2. Architecture

### 2.1 High-Level Architecture

- **Frontend**
  - Angular application for dispatchers, technicians, and admins
- **Backend (Microservices)**
  - `auth-service` – Authentication, authorization, JWT issuance
  - `technician-service` – Technician profiles, skills, availability
  - `ticket-service` – Work orders, tickets, status, SLAs
  - `site-service` – Client locations, sites, device inventory (FA tags)
  - `reporting-service` – Post-mortem reports, exports, metrics
  - `notification-service` – Email/push notifications (optional)
- **API Gateway**
  - Single entry point, routing to services, CORS, rate limiting
- **Database**
  - PostgreSQL (per-service schema or DB-per-service)
- **Infrastructure**
  - Docker & Docker Compose for local dev
  - Container-friendly for deployment (ECS, AKS, GKE, etc.)

### 2.2 Architecture Diagram (Logical)

```mermaid
flowchart LR
    UI[Angular Frontend] --> GW[API Gateway]

    GW --> AUTH[Auth Service]
    GW --> TECH[Technician Service]
    GW --> TKT[Ticket Service]
    GW --> SITE[Site/Client Service]
    GW --> RPT[Reporting Service]
    GW --> NOTIF[Notification Service]

    AUTH --> PG[(Postgres - auth)]
    TECH --> PG2[(Postgres - technicians)]
    TKT --> PG3[(Postgres - tickets)]
    SITE --> PG4[(Postgres - sites)]
    RPT --> PG5[(Postgres - reporting)]

    NOTIF --> MQ[(Message Queue/Email Provider)]
