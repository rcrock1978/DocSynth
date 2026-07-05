# DocSynth

## AI-Powered API Documentation That Stays in Sync With Your Code

---

## What is DocSynth?

DocSynth is a **multi-tenant SaaS platform** that automatically generates, publishes, and maintains human-readable API documentation from your OpenAPI 3.x specifications. The system ingests specs from a URL, file upload, or GitHub repository; produces versioned reference docs with auto-generated code examples; detects structural drift between spec versions; and exposes an interactive "Try It" console that lets API consumers test endpoints directly from the documentation.

The defining characteristic of DocSynth is **continuous synchronization**. Where most documentation tools produce a static artifact at a moment in time and let it decay, DocSynth treats the published documentation as a versioned, immutable snapshot that is re-evaluated on every spec change, with explicit alerts when the live API diverges from what the docs say.

---

## The Problem We Solve

### The Documentation Decay Crisis

API documentation is one of the highest-cost, lowest-priority artifacts in any software organization. Three failure modes are universal:

1. **Stale on arrival.** A spec is written, docs are generated at v1.0, and the engineering team moves on. By v1.3 the docs no longer match the code, but no one notices until a customer files a support ticket.
2. **Drift blindness.** The team has no signal that documentation is out of date. The "spec" lives in a repo, the "docs" live in a separate system, and the relationship between them is implicit and unmanaged.
3. **Drudgery.** Writing and maintaining example code in three languages, building an interactive console, and publishing a navigable reference site is a multi-week project that competes with feature work for engineering time.

The cost of bad documentation is real and measurable: longer time-to-first-call for API consumers, higher support volume, slower enterprise sales cycles, and damage to developer trust in the platform.

### Why Now

Three industry shifts have made this problem tractable for the first time:

- **OpenAPI 3.x is the de facto standard** for REST API contracts. Every modern framework emits it; every API gateway consumes it. The spec is the single source of truth.
- **LLMs can generate syntactically valid code examples** in any major language from a structured input. The cost of "write a cURL example, a Python example, and a Java example for every endpoint" has collapsed.
- **Static-site generation + CDN** makes it economically feasible to publish a separate, immutable, fully-rendered doc site for every spec version, with cache lifetimes measured in days.

DocSynth sits at the intersection of all three.

---

## Who Uses DocSynth

DocSynth serves three distinct user roles within a customer organization:

### 1. Backend Engineers
The engineers who own the API. They submit the OpenAPI spec (URL, file, or GitHub repo), review the generated docs, and configure CI integration so that every push re-validates the docs against the live spec.

**What they get:** docs that are guaranteed to be in sync with the spec, version-tagged automatically, with drift alerts routed to their Slack channel or CI check.

### 2. Docs Owners
The person responsible for the customer-facing documentation site. They manage the active/deprecated/archived lifecycle of each version, configure notification channels, and manage the project membership.

**What they get:** a stable, versioned, public-facing doc site at predictable URLs (`/v1/`, `/v2/`, ...), with clear deprecation signaling and a 90-day wind-down path for retired versions.

### 3. API Consumers
The developers who use the customer's API. They land on a public doc URL, browse the reference, and click "Try It" to test an endpoint from the browser without leaving the documentation.

**What they get:** working code examples, a live console that proxies requests through tenant-scoped security controls, and confidence that what they read matches what the API actually does.

---

## What DocSynth Does

### Five User Stories, Each Independently Deliverable

The product is organized around five user stories, ordered by priority:

#### User Story 1 — OpenAPI Spec Ingestion (P1, MVP)
Submit an OpenAPI 3.x spec from a URL, file upload, or GitHub repository. The system parses, stores, and indexes every endpoint, schema, and security definition. The parsed representation is browsable within 30 seconds of submission.

#### User Story 2 — Endpoint Reference & Example Generation (P1)
From a parsed spec, generate human-readable reference docs. Each endpoint page shows description, parameters, request/response schemas, and at least one working code example in the target language (cURL, Python, Java). The full doc set is generated and browsable within 60 seconds of spec ingestion.

#### User Story 3 — Drift Detection & Alerts (P2)
On every push, compare the live spec against the last published docs. Produce a drift report identifying added, removed, and changed endpoints, with breaking vs non-breaking classification. Send alerts to configured Slack, email, or CI channels. Report is available within 2 minutes of a spec update.

#### User Story 4 — Interactive API Console (P3)
A browser-based "Try It" console on every endpoint reference page. Fill in parameters, click Send, see the live response. Requests are proxied through DocSynth's tenant-scoped security infrastructure, with SSRF protection, allowlist validation, and rate limiting.

#### User Story 5 — Versioned Publishing (P3)
Publish versioned doc sets at stable URLs. Each version is an immutable snapshot. Manage the active/deprecated/archived lifecycle with explicit transition rules. Deprecated versions display a banner; archived versions serve a 410 Gone page after 90 days.

---

## How DocSynth Works

### Architecture at a Glance

DocSynth is built on a strict separation of concerns:

- **Java 21 / Spring Boot 3.x** is the orchestration plane. It owns the multi-tenant data model, the REST API, the outbox-based eventing, and the security perimeter.
- **Python 3.12** is the model plane. A sidecar service runs LangChain and LlamaIndex pipelines for code-example generation and description enhancement. The two communicate over gRPC with JWT-asserted tenant context.
- **Vue 3** is the presentation layer. It is built twice from one codebase: once as a static SSG bundle per versioned DocSet (immutable, CDN-served), and once as the dynamic operator SPA for ingestion, publishing, and console management.
- **PostgreSQL + pgvector** is the data plane. Every row carries a `tenant_id`; row-level security enforces tenant isolation at the database layer.
- **Azure Service Bus** carries asynchronous work (drift detection, doc generation, publishing) via a transactional outbox.
- **Azure Front Door** is the CDN with origin failover and path-prefix purges for state transitions.

### Request Flow — Spec Ingestion

```
User submits spec (URL | file | GitHub repo)
        │
        ▼
[Operator UI] → POST /api/v1/projects/{id}/specs
        │
        ▼
[SpecController] (Spring Boot)
        │  • RBAC check (Editor or Owner)
        │  • Tenant + actor from validated JWT
        ▼
[IngestSpecUseCase]
        │  • Download / clone spec
        │  • Compute SHA-256 (dedup by content)
        │  • Parse with swagger-parser (OpenAPI 3.x, with 2.0→3.0 fallback)
        │  • Persist ApiSpec + Endpoint + Schema rows
        │  • Append "spec.parsed" event to outbox (transactional)
        │  • Emit audit entry
        ▼
[Outbox Relay] → Service Bus: spec.parsed topic
        │
        ├──► [SpecParsedConsumer] (drift re-eval trigger)
        └──► [DriftDetectedConsumer] (if baseline DocSet exists)
```

### Request Flow — Try It Proxy

```
User clicks "Try It" in docs
        │
        ▼
[TryItConsole.vue] → POST /api/v1/proxy/token
        │   (mints short-lived HMAC token bound to user + tenant + target host)
        ▼
[Operator backend]
        │
        ▼
[TryItProxyUseCase]
        │  1. Rate limit check (per-user 60/min, per-tenant 600/min)
        │  2. Verify HMAC token (cross-tenant replay rejected)
        │  3. SSRF guard: reject loopback, RFC1918, link-local, multicast, cloud metadata
        │  4. Allowlist: validate host against project-scoped allowlist
        │  5. Resolve secret from Key Vault (never from browser)
        │  6. Outbound HTTP call (no redirects, capped body/time)
        │  7. Sanitize response headers (strip Authorization, Set-Cookie)
        │  8. Audit emit (NO headers, NO body, NO auth — per proxy audit rules)
        ▼
[Customer API]
        │
        ▼
[Response: status, body, durationMs, requestId]
```

### Request Flow — Drift Detection

```
Trigger: webhook (GitHub push) | scheduled | manual | publish
        │
        ▼
[DetectDriftUseCase]
        │  1. Resolve baseline (latest published DocSet's source ApiSpec)
        │  2. Diff left (newest) vs right (baseline) via openapi-diff-core
        │  3. Classify: breaking | non_breaking | informational
        │     • Structural only (A6 guardrail — no semantic inference)
        │     • Description-only edits → informational
        │     • Required parameter additions → breaking
        │  4. Persist DriftReport + DriftItems
        │  5. Fan out to configured Notifier channels
        │  6. Audit emit
        ▼
[Outbox → Service Bus: drift.detect]
        │
        ▼
Notification channels: Slack | Email | CI check
```

---

## Design Principles

DocSynth is built on five non-negotiable design principles, codified in the project constitution and enforced throughout:

### 1. Spec-Driven Development
The spec is the authoritative source of truth. Code, tests, and documentation derive from and trace back to the spec. Every change starts with a spec update. No implementation is accepted without a corresponding spec entry and passing acceptance criteria.

### 2. Clean Architecture & Domain-Driven Design
Dependency rules point strictly inward: Domain → Application → Infrastructure → Presentation. Bounded contexts (`ingestion`, `documentation`, `drift`, `identity`) are service boundaries. Anti-Corruption Layers protect domain purity from external models, SDKs, and frameworks. ArchUnit tests in CI enforce these boundaries and fail the build on violations.

### 3. Test-First with AI Evaluation
Acceptance criteria are written before implementation. Unit and integration tests follow the Red-Green-Refactor cycle. AI-generated outputs (code examples, descriptions) must pass the eval harness (relevance, faithfulness, task success) before reaching users. Eval thresholds are versioned in `specs/evals/` and gated in CI.

### 4. Observability by Default
Every service emits structured JSON logs (90-day on-disk retention), distributed traces, and RED/USE metrics. Correlation and trace IDs propagate across all service boundaries (Java backend ↔ Python AI sidecar via gRPC). Any production issue without reproducible traces is considered a gap in observability.

### 5. Simplicity & YAGNI
Start with the simplest design that satisfies the current spec. Every dependency must justify its inclusion. Avoid premature abstraction, over-engineering, or building for hypothetical future requirements.

---

## Technology Stack

| Layer | Technology | Rationale |
|---|---|---|
| Backend | Java 21, Spring Boot 3.x | Mature ecosystem; ArchUnit enforces architecture; Spring AI for orchestration |
| AI sidecar | Python 3.12, LangChain 0.3.x, LlamaIndex 0.12–0.13.x | Best-in-class agent and retrieval pipelines |
| Inter-service | gRPC (primary), REST (admin/health) | Streaming support for long-running doc generation; typed schema |
| Database | PostgreSQL 16 + pgvector | Multi-tenant with RLS; vector search for RAG over code examples |
| Frontend | Vue 3, Vite, Vite SSG, Pinia | Two build targets from one codebase (operator SPA + docs SSG) |
| Messaging | Azure Service Bus + transactional outbox | Reliable async; no dual-write problem |
| AuthN/Z | OIDC/OAuth2 + RBAC | Standard, auditable, multi-tenant-safe |
| Secrets | Azure Key Vault | Managed rotation; never in code or logs |
| Observability | Micrometer + OpenTelemetry + Azure Monitor | Vendor-neutral, W3C tracecontext |
| CDN | Azure Front Door | Origin failover, path-prefix purges, geo-distribution |
| Infrastructure | Docker, AKS, Terraform, GitHub Actions | Standard cloud-native deployment |

---

## Security & Compliance

DocSynth is built multi-tenant from the ground up. Security is enforced at every layer.

### Tenant Isolation
Every tenant-scoped table carries a `tenant_id NOT NULL` column. Row-level security policies use a PostgreSQL session variable (`app.current_tenant`) set on every request by the `TenantContextResolver`. The data layer fails closed if the variable is unset. Cross-tenant access is impossible by construction.

### Authentication & Authorization
OIDC/OAuth2 Bearer tokens are validated against the configured provider's JWKS endpoint. Tenant and user identity are derived from the validated JWT claims — never from query parameters or request body. RBAC enforces Owner/Editor/Viewer roles per project (FR-010).

### Secret Management
Secrets (GitHub tokens, API keys for the Try It console, notification credentials) are stored in Azure Key Vault. The database stores only the reference URI, never the value. The proxy resolves secrets at request time and never returns or logs them (FR-011).

### SSRF Protection
The Try It proxy enforces a comprehensive allowlist and SSRF guard:
- URL allowlist per tenant, drawn from OpenAPI `servers[]` and operator-added hosts
- Scheme allowlist (`https` only by default)
- Hostname → IP resolution with IP-class check (rejects loopback, RFC1918, link-local, multicast, IPv6 ULA, cloud metadata IPs)
- Outbound HTTP redirects disabled
- DNS-rebinding mitigation (resolve once, validate, connect to validated IP)
- Header sanitization (strip `Authorization`, `Cookie`, `Proxy-*`, `X-Forwarded-*`)
- Request body cap (1 MB), request timeout (connect ≤ 5s, read ≤ 30s), response size cap (1 MB)
- Per-user (60/min) and per-tenant (600/min) rate limits with `429 + Retry-After`
- Session-bound HMAC request tokens (cross-tenant replay rejected)
- Append-only audit log of every proxy call (no headers, no body, no auth)

### Compliance
Audit log retention: 90 days on-disk, 1 year archive. All operations emit an audit entry with tenant, actor, action, resource, correlation ID, outcome, and detail. The audit log is append-only at the application layer; the database role grants INSERT and SELECT only.

---

## Reliability & Availability

DocSynth targets the following SLAs (SC-007):

- **99.5% monthly uptime** for tenant-facing operations (ingest, generate, publish, drift)
- **RPO ≤ 1 hour**: PostgreSQL Flexible Server with zone redundancy, PITR (35-day retention), geo-redundant backup
- **RTO ≤ 4 hours**: Front Door origin failover (primary/secondary AKS regions), Service Bus geo-disaster-recovery pairing, automated backup verification (weekly restore to scratch, alerting on failure)

### Immutability
Published DocSets are immutable. The bucket prefix is read-only after publish. Any post-publish fix is a new version (`v1.0.0` → `v1.0.1` republished with the fix), never an in-place edit. This makes "is this URL the same as yesterday?" trivially `true`.

### 410 Gone Lifecycle
Archived DocSets are retired on a 90-day wind-down:
- 0–90 days: archived, still browsable
- 90+ days: `gone_at` set by the daily Gone worker; URL serves `410.html` with a link to the current version

---

## Quick Start (Local Development)

```bash
# 1. Bootstrap dev infrastructure
make setup   # Postgres+pgvector, Service Bus emulator, Key Vault emulator, Azurite

# 2. Seed an Owner-role user and a test project
make seed

# 3. Validate each user story
make quickstart-us1   # Submit a public OpenAPI spec
make quickstart-us2   # Generate docs + code examples
make quickstart-us3   # Trigger drift detection
make quickstart-us4   # Round-trip Try It
make quickstart-us5   # Versioned publishing lifecycle

# 4. Teardown
make teardown
```

A complete `specs/001-docsynth-core/quickstart.md` is included in the project repository with detailed runbooks for each scenario.

---

## Repository Layout

```
DocSynth/
├── backend/                 Java 21 / Spring Boot 3.x (orchestration plane)
├── ai-sidecar/              Python 3.12 (model plane; LangChain + LlamaIndex)
├── frontend/                Vue 3 + Vite (operator SPA + docs SSG)
├── contracts/               Cross-language gRPC proto + OpenAPI specs
├── infra/                   Terraform (Azure: AKS, Postgres, Service Bus, Key Vault, Blob, Front Door)
├── specs/                   Feature specifications
│   └── 001-docsynth-core/   Spec, plan, research, data model, contracts, quickstart
└── .github/workflows/       CI: build → test → AI evals → SAST/SCA → container sign → deploy
```

---

## Roadmap

DocSynth v1 ships the five core user stories. Post-launch, the following are prioritized:

| Phase | Capability | Horizon |
|---|---|---|
| v1.1 | AI-generated changelogs from drift reports | Q+1 |
| v1.1 | Custom domain hosting for published doc sets | Q+1 |
| v1.2 | Per-tenant SSO (SAML, Okta, Azure AD) + SCIM | Q+2 |
| v1.2 | On-prem / self-hosted deployment option | Q+2 |
| v2.0 | Non-OpenAPI spec support (AsyncAPI, gRPC, GraphQL) | Q+3 |
| v2.0 | Semantic diff (business-logic change detection) | Q+3 |

Out of scope for v1 (per the spec's Out of Scope section): non-OpenAPI specs, semantic diff, on-prem deployment, custom domain hosting, enterprise SSO/SCIM, and AI-generated changelogs.

---

## Summary

DocSynth is the answer to a problem every API team has: documentation that is always in sync with the spec, always versioned, always testable, and always up. It is built on a clean architecture, observability-first principles, and a security perimeter designed for multi-tenant SaaS. It ships in vertical slices — each user story is independently testable and incrementally deliverable. And it is designed to scale from a single project to a hundred projects per tenant, with five hundred endpoints per project, without degradation.

**For the engineering team:** it eliminates the multi-week documentation project and replaces it with a 30-second ingestion.

**For the docs owner:** it gives a stable, versioned, public-facing doc site with explicit lifecycle management.

**For the API consumer:** it gives working code examples, a live console, and confidence that the docs match the code.
