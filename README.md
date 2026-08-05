# HR & Payroll Management System — Backend API

A secure, multi-branch REST API backend for HR and payroll operations, built for organisations that run several branches and outsource part of their workforce through labour agents. Powers employee management, biometric attendance, leave, loans, statutory-compliant payroll, VAT-aware agent payment calculations, and role-based self-service portals.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security 7 (JWT-based, stateless) |
| ORM | Hibernate / Spring Data JPA |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Batch processing | Spring Batch |
| PDF generation | Apache PDFBox |
| Boilerplate reduction | Lombok |
| Containerisation | Docker (database) |
| Build Tool | Maven |
| Server | Apache Tomcat (embedded) |
| Frontend | Next.js (App Router) — separate repository |

---

## Architecture

The backend follows a strict layered architecture, with each layer further split into per-module sub-packages so the codebase stays navigable as the number of modules grows:

```
HTTP Request
    ↓
Controller        → Accepts/returns DTOs only. Enforces role & scope via @PreAuthorize.
    ↓
Service           → All business logic lives here (interface + impl per module).
    ↓
Repository        → Data persistence and retrieval via Spring Data JPA.
    ↓
Entity            → JPA-mapped database tables.
```

**Mappers** sit between the Service and Entity layers, converting DTOs to Entities and Entities back to response DTOs. Entities never cross the HTTP boundary — every endpoint accepts and returns records from `dto/`, never a JPA entity directly.

**Cross-cutting concerns** — authentication, audit logging, and access scoping — are handled once, centrally, rather than repeated per module:

- `security/` — JWT issuing/validation for user logins, plus a separate device-token filter for biometric hardware, so a compromised branch device can be revoked without touching any user account.
- `audit/AuditAspect` — a single AOP aspect wraps every method annotated `@Auditable` across all modules and writes to the audit log; no module manually calls an audit service.
- `security/AccessGuard` — used inside `@PreAuthorize` expressions everywhere a request needs to be scoped to the caller's own branch, agent, or employee record (e.g. a Branch Manager can query any branch ID in a URL, but `AccessGuard` blocks it unless it's actually theirs).

A deliberate design choice worth calling out: **entity relationships are modelled as plain `Long` foreign key fields, not JPA `@ManyToOne`/`@OneToMany` associations.** This avoids lazy-loading and N+1 query pitfalls, and keeps every entity simple and independently understandable — related data is resolved explicitly in the service layer when a response actually needs it (e.g. an employee response including its branch name), rather than implicitly through the entity graph.

---

## Project Structure

```
src/main/java/com/company/hrpayroll/
│
├── entity/                     JPA entities, one sub-package per module
│   ├── auth/ company/ branch/ department/ agent/ agentadvance/
│   ├── employee/ attendance/ shift/ leave/ loan/
│   ├── payroll/ notification/ audit/
│
├── repository/                 Spring Data JPA interfaces, same module layout
├── dto/
│   ├── request/                 One record per incoming payload, per module
│   └── response/                One record per outgoing payload, per module
├── mapper/                      Entity ↔ DTO conversion, same module layout
│
├── service/
│   ├── auth/ company/ branch/ department/ agent/ agentadvance/
│   ├── employee/ attendance/ shift/ leave/ loan/
│   ├── payroll/
│   │   ├── engine/               Statutory earnings & deduction calculations
│   │   ├── agentformula/          VAT-aware agent payment formula
│   │   ├── batch/                  Payroll run orchestration
│   │   └── documents/               Invoice / voucher / summary PDF generation
│   ├── report/ notification/ audit/ common/
│   └── (each: XxxService interface + impl/XxxServiceImpl)
│
├── controller/                  REST endpoints, same module layout as service/
│
├── security/                    JwtService, JwtAuthFilter, DeviceAuthFilter,
│                                 SecurityConfig, CustomUserDetailsService, AccessGuard
├── audit/                        AuditAspect + @Auditable annotation
├── event/                         Domain events (PayrollCompletedEvent, LeaveApprovedEvent…)
├── config/                        AsyncConfig (virtual threads), CORS, Swagger
├── exception/                     GlobalExceptionHandler + custom exceptions
│
└── HrpayrollApplication.java
```

```
src/main/resources/
├── application.yml
└── db/migration/                  Flyway SQL migrations — single source of truth for schema
```

---

## Domain Overview

| Domain | Description |
|---|---|
| **Auth** | Login, JWT issuing/refresh, user accounts across all nine roles |
| **Company & Settings** | Company profile, statutory numbers, configurable VAT rate |
| **Branches** | Unlimited branches, each with an assigned manager |
| **Departments** | Grouped under a branch, with an optional head of department |
| **Agents** | Labour agents supplying outsourced staff, with configurable charge type and VAT-registered flag |
| **Agent Advances** | Money advanced to agents, recovered automatically during payroll |
| **Employees** | Full personal, government, employment, and bank records, plus document uploads |
| **Attendance** | Manual entry and fingerprint-based biometric attendance, synced from branch hardware |
| **Shifts** | Morning/evening/night/rotating/flexible, assignable by employee, department, or branch |
| **Leave** | Request → manager approval → HR approval workflow |
| **Loans** | Employee loan requests, approval, and automatic payroll deduction |
| **Payroll** | Statutory-compliant earnings/deduction calculation and full payroll runs |
| **Agent Payroll** | VAT-aware formula producing the final amount payable per agent |
| **Reports** | Attendance, payroll register, and employee list, exportable as PDF |
| **Notifications** | Email, in-app, and (pluggable) SMS, triggered by domain events |
| **Audit Trail** | Every significant write action logged automatically via AOP |

---

## API Endpoints

### Auth `/auth`, `/users`
| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/login` | Login — used by all three frontend surfaces (staff/agent/employee) |
| POST | `/auth/refresh` | Refresh an access token |
| POST | `/users` | Create a user account (Super Admin only) |
| GET | `/users` | List users |
| PUT | `/users/{id}/change-password` | Change password |
| PUT | `/users/{id}/deactivate` | Deactivate a user |

### Company `/company`
| Method | Endpoint | Description |
|---|---|---|
| GET / PUT | `/company` | Company profile |
| GET / PUT | `/company/settings` | Statutory settings, including the VAT rate |

### Branches `/branches`
| Method | Endpoint | Description |
|---|---|---|
| POST / PUT | `/branches`, `/branches/{id}` | Create / update a branch |
| GET | `/branches`, `/branches/{id}`, `/branches/my-branch` | List / fetch a branch |
| PUT | `/branches/{id}/deactivate` | Deactivate a branch |

### Departments `/departments`
| Method | Endpoint | Description |
|---|---|---|
| POST / PUT | `/departments`, `/departments/{id}` | Create / update a department |
| GET | `/departments/{id}`, `/departments/by-branch/{branchId}` | Fetch department(s) |
| DELETE | `/departments/{id}` | Delete a department |

### Agents `/agents`, `/agent-advances`
| Method | Endpoint | Description |
|---|---|---|
| POST / PUT | `/agents`, `/agents/{id}` | Create / update an agent |
| GET | `/agents`, `/agents/{id}`, `/agents/me` | List / fetch agent(s) |
| PUT | `/agents/{id}/deactivate` | Deactivate an agent |
| POST | `/agent-advances` | Record an advance |
| PUT | `/agent-advances/{id}/recover` | Manually recover part of an advance |
| GET | `/agent-advances/by-agent/{agentId}`, `/agent-advances/my-advances` | View advances |

### Employees `/employees`
| Method | Endpoint | Description |
|---|---|---|
| POST / PUT | `/employees`, `/employees/{id}` | Create / update an employee |
| GET | `/employees/{id}`, `/by-branch/{id}`, `/by-agent/{id}`, `/by-department/{id}`, `/me` | Fetch employee(s) |
| PUT | `/employees/{id}/terminate` | Terminate an employee |
| POST / GET / DELETE | `/employees/{id}/documents` | Upload / list / delete documents |

### Attendance `/attendance`
| Method | Endpoint | Description |
|---|---|---|
| POST | `/attendance/clock-in` | Manual clock-in |
| PUT | `/attendance/{id}/clock-out` | Manual clock-out |
| GET | `/attendance/employee/{id}`, `/attendance/branch/{id}`, `/attendance/my-attendance` | View records |
| POST | `/attendance/devices` | Register a branch biometric device (issues a device token) |
| GET | `/attendance/devices/by-branch/{branchId}` | List devices |
| POST | `/attendance/enrollment` | Enroll a fingerprint template |
| POST | `/attendance/biometric/sync` | **Device-token authenticated** — the endpoint the bridge-agent calls |
| GET | `/attendance/biometric/templates` | **Device-token authenticated** — branch template cache pull |

### Shifts `/shifts`
| Method | Endpoint | Description |
|---|---|---|
| POST / PUT | `/shifts`, `/shifts/{id}` | Create / update a shift |
| GET | `/shifts` | List shifts |
| POST | `/shifts/assignments` | Assign a shift to employee, department, or branch |
| GET | `/shifts/assignments/employee/{id}`, `/shifts/assignments/branch/{id}` | View assignments |

### Leave `/leave`
| Method | Endpoint | Description |
|---|---|---|
| GET | `/leave/types` | List leave types |
| POST | `/leave/apply` | Employee applies for leave |
| PUT | `/leave/{id}/approve-by-manager`, `/leave/{id}/approve-by-hr` | Two-stage approval |
| GET | `/leave/employee/{id}`, `/leave/pending/manager`, `/leave/pending/hr`, `/leave/my-balance` | View leave data |

### Loans `/loans`
| Method | Endpoint | Description |
|---|---|---|
| POST | `/loans`, `/loans/request` | Request a loan |
| PUT | `/loans/{id}/approve`, `/loans/{id}/reject` | Approve / reject |
| GET | `/loans/employee/{id}`, `/loans/my-loans` | View loans |

### Payroll `/payroll`
| Method | Endpoint | Description |
|---|---|---|
| POST | `/payroll/run` | Run payroll by branch, agent, department, or all employees |
| GET | `/payroll/runs/{id}`, `/payroll/runs/{id}/payslips`, `/payroll/runs/{id}/agent-summaries` | View run results |
| GET | `/payroll/runs/{id}/agents/{agentId}/invoice` | Download Agent Invoice (PDF) |
| GET | `/payroll/runs/{id}/agents/{agentId}/voucher` | Download Payment Voucher (PDF) |
| GET | `/payroll/runs/{id}/agents/{agentId}/summary` | Download Agent Payroll Summary (PDF) |

### Reports `/reports`
| Method | Endpoint | Description |
|---|---|---|
| GET | `/reports/attendance/{branchId}` | Attendance report (PDF) |
| GET | `/reports/payroll-register/{payrollRunId}` | Payroll register (PDF) |
| GET | `/reports/employee-list` | Employee list report (PDF) |

### Notifications & Audit `/notifications`, `/audit-logs`
| Method | Endpoint | Description |
|---|---|---|
| GET | `/notifications`, `/notifications/unread-count` | View own notifications |
| PUT | `/notifications/{id}/read` | Mark as read |
| GET | `/audit-logs/entity/{type}/{id}`, `/audit-logs/user/{userId}` | Read-only audit trail (Super Admin / Auditor) |

---

## Database

**Engine:** PostgreSQL 16, running in Docker
**Database name:** `hrpayroll`
**Schema management:** Flyway migrations only — `ddl-auto: validate`, not `update`. Hibernate checks the schema matches the entities at startup but never alters it; every schema change goes through a versioned migration file in `db/migration/`, which keeps schema history reviewable and reproducible across environments.

### Entity relationships (via plain foreign key fields, not JPA associations)

```
Branch ──────────────────┬─→ Department
                          ├─→ Employee
                          └─→ BiometricDevice

Agent ────────────────────┬─→ Employee (outsourced staff)
                          └─→ AgentAdvance

Employee ─────────────────┬─→ Attendance
                          ├─→ BiometricTemplate
                          ├─→ LeaveRequest
                          ├─→ Loan
                          ├─→ EmployeeDocument
                          └─→ Payslip

PayrollRun ───────────────┬─→ Payslip
                          └─→ AgentPayrollSummary

User ──────────────────── (optionally links to) Employee or Agent
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven
- Docker Desktop

### 1. Clone the repository

```bash
git clone <repository-url>
cd hrpayroll
```

### 2. Start PostgreSQL via Docker Compose

A `docker-compose.yml` and `.env` sit at the project root:

```bash
docker compose up -d
```

### 3. Configure `application.yml`

Update `src/main/resources/application.yml` to match your local database port and credentials, and set a JWT signing secret (32+ characters):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/hrpayroll
    username: hrpayroll_user
    password: your_password
  jpa:
    hibernate:
      ddl-auto: validate

app:
  jwt:
    secret: your-32-character-minimum-secret
```

### 4. Run the application

```bash
mvn spring-boot:run
```

Or run `HrpayrollApplication` directly from IntelliJ IDEA.

The API will be available at `http://localhost:8080`. Flyway runs automatically on startup and seeds a default Super Admin account for first login (password reset required on first login).

---

## Security Model

- **JWT-based, stateless** — no server-side session state; every request carries a bearer token.
- **Three frontend login surfaces** (staff, agent, employee) share one backend `/auth/login` — the audience separation is a frontend concern only; there is exactly one place credentials are ever checked.
- **Role-based + scope-based access control** — nine roles (Super Admin, Admin, HR Manager, Payroll Officer, Branch Manager, Agent, Employee, Finance Officer, Auditor), enforced via `@PreAuthorize` at the controller layer, backed by `AccessGuard` for branch/agent/employee-level scoping.
- **Biometric devices authenticate separately** from users, via a per-device token issued at registration, so a compromised branch device can be revoked without affecting any user account.
- **Every significant write action is logged automatically** to the audit trail via a single AOP aspect — no module has to remember to call an audit service manually.

---

## Frontend

This backend is consumed by the **HR & Payroll** frontend, built with **Next.js** (App Router), split into role-based route groups (Super Admin, Admin, Branch Manager, Agent Portal, Employee Self-Service) and three separate login surfaces for staff, agents, and employees.

A separate local bridge application (installed per branch) integrates the SecuGen fingerprint reader, matching locally and syncing attendance events to this backend's device-authenticated endpoints.

Frontend repo: _link to be added_

---

## Development Notes

- Lombok is used throughout (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`) to remove boilerplate on entities, controllers, and services
- DTOs are Java records — request and response types are always separate, one file per module
- Mappers are annotated `@Service`, converting Entity ↔ DTO only — no business logic lives in a mapper
- Constructor injection is used throughout — no field injection
- Entity relationships are plain `Long` foreign key fields, not JPA `@ManyToOne`/`@OneToMany` — resolved explicitly in the service layer, not implicitly through the entity graph
- Entities never cross the HTTP boundary; only DTOs are exposed
- Virtual threads (`spring.threads.virtual.enabled`) are used for I/O-bound work — every incoming HTTP request and all `@Async` notification dispatch — since that work is I/O-bound, not CPU-bound

---

## Author

**[Your Name]**
Full-Stack Developer

---

## License

This project is proprietary software.
