# Dhwarsh Employee Management: architecture and compliance record

How the system is built, which decisions were taken and why, and where it stands against the
*Enterprise EMS Detailed Architecture & Implementation Guide*. Keep this file current when something changes.

## 1. Shape of the system

| Layer | What we use | Where |
|---|---|---|
| Web app | Next.js + TypeScript, one Vercel project per organisation | `dhwarsh-hr-frontend` |
| API | Spring Boot, Spring Security (stateless JWT), one deployment per organisation | this repo |
| Data | PostgreSQL 16, one database per organisation; Flyway migrations | `src/main/resources/db/migration` |
| Supporting | Redis (login/reset throttling), Docker, GitHub Actions, Tailscale Funnel (HTTPS) | `docker-compose.prod.yml`, `.github/workflows/deploy.yml` |
| Branch PCs | WPF desktop app with the fingerprint reader; signed auto-update | `HrpayrollDesktop` repo |

The API is the only authority: every endpoint checks the role (`@PreAuthorize`) and, where the record
belongs to a branch or an agent, that the caller may see that branch or agent (`accessGuard`). The web app
hides what a role cannot do, but never relies on that.

## 2. Decisions (ADRs)

**ADR-001 One database and one backend per organisation (instead of shared tables with `tenant_id`).**
*Context:* Kisiimatt and Shah Ceramics must never see each other's data; Kisiimatt also hosts the server.
*Decision:* each organisation gets its own folder on the VM (`~/kisiimatt`, `~/shahceramics`), its own
Postgres volume, Redis, backend container, secrets (`JWT_SECRET`, `CREDENTIAL_KEY`, `BIOMETRIC_TEMPLATE_KEY`)
and its own Vercel project. *Consequences:* isolation is physical, so a missing `tenant_id` filter cannot
leak data and a token from one organisation is rejected by the other (different `JWT_SECRET`). The cost is
one deploy per organisation, which the pipeline does in a loop. Revisit (shared tables + Row-Level Security,
guide 2.2) only if organisations grow into the dozens.

**ADR-002 Modular monolith.** One Spring Boot app, packaged by capability (employee, attendance, settlement,
damage, audit, ...), each with controller / service / repository layers. No microservices, queues or
Kubernetes until a measured need appears (guide 12).

**ADR-003 Optimistic locking for employee records (V34).** `employees.version`; the edit form sends the version
it opened, and a stale save is refused with 409 instead of overwriting someone else's change. Extend to other
records that several people edit at once if that need appears.

**ADR-004 Every request has an ID.** `RequestIdFilter` puts it on every log line, in the `X-Request-Id`
response header, and in "unexpected error" messages ("Reference ..."), so a user's report leads straight to
the log lines.

**ADR-005 Branch PCs are tied to one organisation.** Each PC's server is stored in
`C:\ProgramData\Dhwarsh HR\server.json`; updates never change it. Organisation installers
(`Kisiimatt-Setup`, `ShahCeramics-Setup`) set it; a silent install never moves a PC to another organisation.

**ADR-006 Several PCs per branch (two Back Office desks).** Any number of PCs can be activated for a branch by
its Back Office or Branch Manager; each has its own key (`<reader>@<computer>` for new activations), so one never
cancels another. A scan is decided from the branch's whole day: each PC pulls `GET /attendance/biometric/today`
(every PC's steps and manual check-ins, with the PCs' own event ids) before deciding, and merges it with its own
unsent scans. The server's state machine is per employee, and a step that has already happened (the other PC
got there first) is accepted without recording it again, so nothing is duplicated or refused. Offline, a PC
decides from what it has, and the server still absorbs any repeat when it syncs.

**ADR-007 One staff sign-in on the branch PC.** The Back Office and the Branch Manager both open the console
(the manager can stand in when the Back Office is absent); server-side they are limited to their own branch.
Manual check-in is in the console only for the Branch Manager (server: `hasRole('BRANCH_MANAGER')`).

## 3. Compliance with the guide

✅ in place · 🟡 partly / acceptable for now · ⏳ recommended next · ➖ not needed yet (guide says add on evidence)

| Guide | Requirement | Status | How / notes |
|---|---|---|---|
| 1 | Backend is the security authority | ✅ | `@PreAuthorize` on every controller; branch/agent scope checks; web app never trusted |
| 2.1 | Keys, FKs, UNIQUE, NOT NULL | ✅ | Flyway schema; unique employee numbers, national IDs, M-Pesa codes |
| 2.2 | Tenant isolation | ✅ | Database-per-organisation (ADR-001); separate JWT secrets |
| 2.3 | Transactions, concurrency | ✅ | `@Transactional` services; optimistic locking on employees (ADR-003) |
| 2.4 | Indexes, pagination | 🟡 | Lookup indexes (V32); lists are small per branch; add paging when a list exceeds a few hundred rows |
| 2.5 | Versioned migrations, expand-and-contract | ✅ | Flyway; V34 is additive; big changes staged in `db-pending/` and applied only after a backup |
| 3 | Modular monolith, thin controllers, DTOs | ✅ | ADR-002; entities never returned directly |
| 3.3 | Idempotent payments / approvals | ✅ | DB uniqueness on M-Pesa code, settlement period, payslip, days-worked period; violations now answer 409, not 500 |
| 4.1 | Password hashing, secure sessions | ✅ | BCrypt; web session in an httpOnly, Secure, SameSite cookie; 15-min access tokens |
| 4.2 | RBAC + object-level checks | ✅ | Roles + `accessGuard` (same branch / same agent) |
| 4.3 | Rate limits | ✅ | Login lockout (5 tries / 15 min) and reset throttling in Redis |
| 4.3 | Security headers | ✅ | API: Spring Security defaults. Web: HSTS, nosniff, frame-ancestors, Referrer- and Permissions-Policy |
| 4.3 | No secrets in code | 🟡 | All production secrets in each server's `.env`. The old JWT fallback is still in `application.yaml` for Kisiimatt; the app now warns at start-up. **Action:** set `JWT_SECRET` in `~/kisiimatt/.env` |
| 4.3 | Upload checks | ✅ | Logo checked by file signature and size; uploads now kept in a volume (they were lost on every redeploy) |
| 4.4 | Audit trail | ✅ | `audit_logs`, append-only by database trigger; actor, action, target, time; request ID in logs |
| 5.1 | Timeouts / retries | 🟡 | Desktop app queues scans offline and retries; mail sending is time-boxed |
| 5.4 | Backups + restore drill | ✅ | `deploy/backup.sh` (nightly, per organisation, DB + uploads), `deploy/restore-check.sh` (weekly drill). **Action:** install the cron lines and an off-server copy (RUNBOOK §3) |
| 6 | Pooling, measured tuning | ✅ | HikariCP pool 15, indexes from measured slow lookups |
| 7 | Automated tests in CI | 🟡 | 68 unit/integration tests block every deploy; browser end-to-end checks run before releases. ⏳ Testcontainers, load test (k6) |
| 8.2 | CI/CD with health verification | ✅ | Tests → image → per-organisation deploy that waits for a healthy container and smoke-tests it; stops on the first failure; `HOLD` file per organisation |
| 8.3 | Containers: non-root, health checks, limits | ✅ | Image runs as user `app`; health check; 1 GB memory cap; logs capped |
| 8.1 | Separate staging environment | ⏳ | Today: local throwaway database copies. Recommended: a third stack on the VM (`~/staging`) |
| 9.1 | Logs with request IDs | ✅ | ADR-004 |
| 9.1 | Metrics and alerts | ⏳ | Recommended first step: a free uptime monitor on each organisation's `/public/organisation`, alerting by e-mail |
| 9.3 | Data Protection Act 2019 | ⏳ | Needs advice from a qualified person: retention periods, data-subject requests, registration with the ODPC |
| 12 | Microservices, queues, sharding, Kubernetes | ➖ | Not justified by current load (ADR-002) |

## 4. The seven questions for every feature (guide 14)

Before merging a feature, answer: can retries or two users at once corrupt it? Can it reach another person's
or organisation's data? What if the database or reader is offline? How does it behave at 10x the data? Can
someone else change it safely? Will the logs show why it failed? Can it be restored from backup?
