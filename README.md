# ClearFund — Investment Fund Order Routing & Custody Simulator

A Spring Boot 3 / Java 21 backend that simulates **subscription and redemption
orders** for investment funds: order routing through a lifecycle state
machine, NAV-based pricing, T+2 settlement simulation, custody holdings, cash
balances, full audit logging, and a mock SWIFT-style message intake.

> Educational portfolio project — no real trading, real SWIFT, or real
> financial calculations.

## Tech stack

| Area | Choice |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3 (Web, Data JPA, Validation) |
| Database | PostgreSQL (local), Oracle-compatible SQL design |
| Migrations | Flyway (`V1`–`V3`) |
| Build | Maven |
| Tests | JUnit 5, Mockito, MockMvc, Testcontainers |
| DevOps | Docker, Docker Compose, Jenkins |

See also: [TESTING.md](TESTING.md) and [docs/api-examples.md](docs/api-examples.md).

## Architecture

Clean layered design under `com.clearfund`: `controller → service →
repository → entity`, with `dto`, `mapper`, `exception`, `audit`, `enums` and
`config` packages. The order lifecycle is an explicit state machine:

```
RECEIVED → VALIDATED → ROUTED → ACCEPTED → SETTLEMENT_PENDING → SETTLED
   └────────────┴───────────┴──→ REJECTED / CANCELLED
```

Settlement (cash/units movement) is owned by `SettlementService` and runs in a
single rollback-safe `@Transactional` unit.

## Local setup

### Option A — Docker (recommended, zero local tooling)

Requires only Docker.

```bash
scripts/run-local.sh            # build + start db and backend (foreground)
scripts/run-local.sh --detach   # background
# Windows:
./scripts/run-local.ps1
```

Backend: <http://localhost:8080>. Try it:

```bash
curl http://localhost:8080/api/system/health-summary
curl http://localhost:8080/api/orders
```

### Option B — Run the JVM locally against a Postgres container

Requires JDK 21 + Maven.

```bash
docker compose up -d db                       # just the database
mvn spring-boot:run                           # app on the host
```

The app defaults to `jdbc:postgresql://localhost:5432/clearfund`
(user/password `clearfund`), matching the compose database.

### Running the tests

```bash
scripts/run-tests.sh                                  # everything
scripts/run-tests.sh '-Dtest=!OrderLifecycleIntegrationTest'   # skip Docker test
./scripts/run-tests.ps1                               # Windows
```

The Testcontainers integration test is **skipped (not failed)** when Docker is
unavailable. Full details in [TESTING.md](TESTING.md).

## Docker setup

| File | Purpose |
|---|---|
| `Dockerfile` | Multi-stage: Maven build → slim `temurin:21-jre` runtime, non-root user |
| `docker-compose.yml` | `db` (PostgreSQL 16) + `backend`; backend waits on the db healthcheck |
| `.dockerignore` | Keeps `target/`, `.git/` etc. out of the build context |

The backend image runs the packaged jar; `SPRING_DATASOURCE_URL` is
overridden in compose so the container reaches the `db` service by hostname.
Flyway runs the migrations on startup; Hibernate is in `validate` mode.

```bash
docker compose up --build      # start
docker compose down            # stop (keep data)
docker compose down -v         # stop and wipe the database volume
```

## Jenkins pipeline

`Jenkinsfile` is a declarative pipeline with these stages:

1. **Checkout** — `checkout scm`.
2. **Build** — `mvn -B -DskipTests clean compile` (fail fast on compilation).
3. **Unit tests** — `mvn -B test`; results published via `junit`. The
   Testcontainers test self-skips on agents without Docker.
4. **Static analysis** — placeholder stage; wire in SpotBugs / Checkstyle /
   SonarQube here (the exact command is noted in a comment).
5. **Package** — `mvn -B -DskipTests package`; the jar is archived with
   `fingerprint: true` for traceability.

Pipeline options: `timestamps()`, a 20-minute `timeout`, and
`disableConcurrentBuilds()`. It assumes JDK 21 + Maven on the agent (or
configure them via Jenkins Global Tool Configuration and a `tools {}` block).

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `scripts/run-local.sh` exits with "Docker does not appear to be running" | Start Docker Desktop / the Docker daemon. |
| Backend exits with `Connection refused` to the db | The db wasn't healthy yet. Compose waits on the healthcheck; if running the JVM locally, ensure `docker compose up -d db` finished and port 5432 is free. |
| `Port 8080 (or 5432) already allocated` | Another process is using it. Stop it, or change the host port mapping in `docker-compose.yml`. |
| Flyway `validate`/migration error after editing an applied migration | Don't edit an applied migration. For local dev, `docker compose down -v` to wipe the volume and re-migrate. |
| Hibernate `Schema-validation` failure on startup | An entity and its migrated column drifted. Align the entity `@Column` with the `Vx` DDL (this is intentional — `ddl-auto: validate` catches drift early). |
| Testcontainers test fails to start a container | Docker not running or no image pull access. The test is designed to **skip** without Docker — run `scripts/run-tests.sh '-Dtest=!OrderLifecycleIntegrationTest'` to exclude it explicitly. |
| `mvn: command not found` in Jenkins | Configure Maven/JDK in Jenkins Global Tool Configuration and add a `tools {}` block, or install them on the agent. |
| PowerShell "running scripts is disabled" | `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` then re-run. |
