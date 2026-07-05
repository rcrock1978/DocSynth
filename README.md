<div align="center">

# DocSynth

### AI-Powered API Documentation That Stays in Sync With Your Code

[![Status](https://img.shields.io/badge/status-v1.0%20shipped-10b981?style=for-the-badge)](specs/001-docsynth-core/tasks.md)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](backend/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](backend/)
[![Python 3.12](https://img.shields.io/badge/Python-3.12-3776AB?style=for-the-badge&logo=python&logoColor=white)](ai-sidecar/)
[![Vue 3](https://img.shields.io/badge/Vue-3-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](frontend/)
[![PostgreSQL 16 + pgvector](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-336791?style=for-the-badge&logo=postgresql&logoColor=white)](infra/modules/postgres/)
[![Azure](https://img.shields.io/badge/Azure-AKS%20%2B%20Service%20Bus%20%2B%20Front%20Door-0078D4?style=for-the-badge&logo=microsoftazure&logoColor=white)](infra/)

**Multi-tenant SaaS that ingests OpenAPI 3.x specs, generates versioned human-readable API reference docs, detects structural drift, exposes an interactive "Try It" console, and publishes immutable versioned doc sets at stable URLs.**

[Quick Start](#-quick-start) · [Architecture](#-architecture) · [Five Capabilities](#-five-capabilities) · [Documentation](#-documentation) · [Roadmap](#-roadmap)

</div>

---

## The Problem

Every API team has the same problem: **the documentation is always wrong.** Stale on arrival, drift is invisible, and the only signal that something broke is a customer support ticket. Bad documentation costs hours of support time per customer, slows enterprise sales, and damages developer trust in the platform.

## The Solution

DocSynth sits between your OpenAPI spec and your customers. The spec is the single source of truth; everything else — reference docs, code examples, drift alerts, the Try It console — is derived from it and continuously synchronized.

```
┌──────────────┐    ┌────────────┐    ┌──────────────────────────────────────┐
│  OpenAPI 3.x │ ─► │  DocSynth  │ ─► │  • Versioned reference docs           │
│   spec       │    │            │    │  • Auto-generated code examples      │
└──────────────┘    │  (30s)     │    │  • Drift detection on every push      │
                    │  ingest    │    │  • "Try It" console (SSRF-hardened)  │
                    │            │    │  • 90-day wind-down for retired APIs  │
                    └────────────┘    └──────────────────────────────────────┘
```

---

## ✨ Five Capabilities

| # | Capability | Priority | What it does | SLA |
|---|---|---|---|---|
| 1 | **OpenAPI Spec Ingestion** | P1 · MVP | Submit from URL, file, or GitHub repo; parsed representation within 30 s | SC-001 |
| 2 | **Reference & Example Generation** | P1 | Human-readable docs + code examples in cURL/Python/Java within 60 s | SC-002 |
| 3 | **Drift Detection & Alerts** | P2 | Slack/email/CI alerts on every push, classified breaking vs non-breaking | SC-003 |
| 4 | **Interactive "Try It" Console** | P3 | Browser → live API call through a tenant-scoped, allowlist-validated proxy | SC-004 |
| 5 | **Versioned Publishing** | P3 | `/v1/`, `/v2/`, ... with active/deprecated/archived lifecycle + 410 Gone | FR-014 |

**All 147 implementation tasks complete.** Each capability is an independently testable, deployable vertical slice.

---

## 🏗️ Architecture

**Three planes, strict separation of concerns.**

```
                    ┌────────────────────────────────────────────┐
                    │       Presentation plane (Vue 3)           │
                    │   Operator SPA  ·  Docs SSG (per version)  │
                    └──────────┬────────────────┬────────────────┘
                               │ REST           │
                               │ gRPC           │
                    ┌──────────▼────────────────▼────────────────┐
                    │       Orchestration plane (Java 21)       │
                    │  Spring Boot 3.x · Multi-tenant · RLS ·     │
                    │  OIDC+RBAC · Outbox · OTel · Audit          │
                    └──────────┬────────────────┬────────────────┘
                               │ gRPC (JWT-asserted tenant context)
                    ┌──────────▼────────────────────────────────┐
                    │         Model plane (Python 3.12)          │
                    │  LangChain 0.3.x  ·  LlamaIndex 0.12–0.13  │
                    └────────────────────────────────────────────┘
                               │
                    ┌──────────▼────────────────────────────────┐
                    │   PostgreSQL 16 + pgvector · Service Bus  │
                    │   Blob Storage · Key Vault · Front Door   │
                    └────────────────────────────────────────────┘
```

| Layer | Technology | Why |
|---|---|---|
| Backend | Java 21, Spring Boot 3.x, Spring AI | Mature ecosystem, ArchUnit-enforced architecture |
| AI sidecar | Python 3.12, LangChain 0.3.x, LlamaIndex 0.12–0.13.x | Best-in-class agent + retrieval pipelines |
| Inter-service | gRPC (primary), REST (admin/health) | Streaming, typed schema |
| Frontend | Vue 3, Vite, Vite SSG, Pinia | Two build targets from one codebase |
| Database | PostgreSQL 16 + pgvector | Multi-tenant with RLS, vector search |
| Messaging | Azure Service Bus + transactional outbox | Reliable async, no dual-write |
| AuthN/Z | OIDC/OAuth2 + RBAC (Owner / Editor / Viewer) | Standard, auditable, multi-tenant-safe |
| Secrets | Azure Key Vault | Managed rotation, never in code or logs |
| Observability | OpenTelemetry + Micrometer + Azure Monitor | Vendor-neutral, W3C tracecontext |
| CDN | Azure Front Door | Origin failover, path-prefix purges |
| Infrastructure | Docker, AKS, Terraform, GitHub Actions | Standard cloud-native |

---

## 🚀 Quick Start

```bash
# 1. Bootstrap local infrastructure
make setup   # Postgres+pgvector, Service Bus emulator, Key Vault emulator, Azurite
make seed    # Dev tenant + project + Owner-role user

# 2. Validate each user story end-to-end
make quickstart-us1   # Submit a public OpenAPI spec
make quickstart-us2   # Generate docs + code examples
make quickstart-us3   # Trigger drift detection
make quickstart-us4   # Try It round-trip with a stub API
make quickstart-us5   # Versioned publishing lifecycle

# 3. Teardown
make teardown
```

See [`specs/001-docsynth-core/quickstart.md`](specs/001-docsynth-core/quickstart.md) for the complete runbook.

### Prerequisites

- **Java 21** — `sdk use java 21.x` (`.sdkmanrc` pinned)
- **Python 3.12** — (`ai-sidecar/.python-version` pinned)
- **Node 20+** (`frontend/.nvmrc` pinned)
- **Terraform ≥ 1.9**
- **Docker** (for local Postgres + Service Bus emulator)

---

## 📂 Repository Layout

```
DocSynth/
├── backend/                  # Java 21 / Spring Boot 3.x (orchestration plane)
│   ├── src/main/java/com/docsynth/
│   │   ├── domain/          # Domain models, value objects, domain events
│   │   ├── application/     # Use cases, application services, ports
│   │   ├── infrastructure/  # JPA, gRPC, Service Bus, Key Vault, OIDC
│   │   ├── interfaces/      # REST + gRPC controllers
│   │   └── config/
│   ├── src/main/resources/
│   │   ├── db/migration/    # Flyway migrations (V001–V005)
│   │   └── logback-spring.xml
│   └── src/test/            # JUnit 5, ArchUnit, Testcontainers
│
├── ai-sidecar/               # Python 3.12 (model plane)
│   ├── src/docsynth_ai/
│   │   ├── pipelines/       # LangChain code-gen + description enhance
│   │   ├── prompts/         # Versioned prompt templates (YAML)
│   │   ├── observability/   # OTel, JSON logs, RED metrics
│   │   └── server.py        # gRPC server with RS256 JWKS auth
│   └── tests/               # pytest
│
├── frontend/                 # Vue 3 + Vite (two build targets)
│   ├── src/operator/        # Dynamic SPA: ingest, publish, manage
│   ├── src/docs/            # Pre-rendered DocSet runtime
│   ├── src/shared/types/    # Cross-target TypeScript types
│   └── e2e/                 # Playwright
│
├── contracts/                # Cross-language gRPC proto + OpenAPI specs
│   ├── proto/               # ingestion.proto, ai_orchestration.proto, drift.proto
│   └── openapi/             # operator-api.yaml, proxy-api.yaml
│
├── infra/                    # Terraform (Azure: AKS, Postgres, Service Bus, Key Vault, Blob, Front Door)
│   ├── modules/
│   ├── envs/{dev,staging,prod}/
│   └── runbooks/
│
├── specs/                    # Feature specifications
│   ├── 001-docsynth-core/   # Spec, plan, research, data model, contracts, quickstart
│   └── evals/               # AI eval thresholds (Constitution Principle III)
│
├── docs/                     # Stakeholder documentation
│   ├── README.md            # Comprehensive overview (you are here)
│   ├── linkedin-pitch.md    # 120-second pitch
│   └── presentation.md      # 18-slide stakeholder deck
│
└── .github/workflows/        # CI: build → test → AI evals → SAST/SCA → container sign → deploy
```

---

## 🔐 Security

| Concern | Posture |
|---|---|
| **Tenant isolation** | Row-level security at the database layer; cross-tenant access fails closed |
| **Authentication** | OIDC/OAuth2 Bearer tokens validated against JWKS; tenant derived from claims |
| **Authorization** | RBAC: Owner, Editor, Viewer per project (FR-010) |
| **Secrets** | Stored in Azure Key Vault; database stores references only; never logged (FR-011) |
| **SSRF protection** | Allowlist + IP-class check + redirect disable + DNS-rebinding mitigation + rate limit |
| **Audit** | Append-only log; INSERT and SELECT only at the DB role level; 90-day retention |
| **Outbound proxy** | HMAC tokens bound to (user, tenant, target host); cross-tenant replay rejected |
| **Compliance** | SOC2 Type II in progress; security pack available on request |

---

## 📊 Reliability (SC-007)

| Target | Value | How |
|---|---|---|
| Monthly uptime | **99.5%** | Front Door active-active origins with health-probed failover |
| RPO | **≤ 1 hour** | PostgreSQL PITR (35-day retention) + geo-redundant backup |
| RTO | **≤ 4 hours** | Front Door origin failover + Service Bus geo-DR pairing + automated backup verification |

---

## 📚 Documentation

### For developers
- [**spec.md**](specs/001-docsynth-core/spec.md) — what we're building (authoritative source of truth)
- [**plan.md**](specs/001-docsynth-core/plan.md) — how we're building it
- [**research.md**](specs/001-docsynth-core/research.md) — 4 architectural decisions (Try It proxy, drift detection, AI orchestration, SSG-hybrid publishing)
- [**data-model.md**](specs/001-docsynth-core/data-model.md) — 14 entities with row-level security
- [**contracts/**](specs/001-docsynth-core/contracts/) — gRPC + REST contracts
- [**quickstart.md**](specs/001-docsynth-core/quickstart.md) — end-to-end validation scenarios

### For stakeholders
- [**docs/README.md**](docs/README.md) — comprehensive project overview
- [**docs/linkedin-pitch.md**](docs/linkedin-pitch.md) — 120-second pitch
- [**docs/presentation.md**](docs/presentation.md) — 18-slide stakeholder deck

### Governance
- [**constitution.md**](.specify/memory/constitution.md) — five binding principles:
  1. **Spec-Driven Development** — every change starts with a spec update
  2. **Clean Architecture & DDD** — Domain → Application → Infrastructure → Presentation
  3. **Test-First with AI Evaluation** — eval thresholds gated in CI
  4. **Observability by Default** — structured logs, RED/USE metrics, distributed traces
  5. **Simplicity & YAGNI** — every dependency must justify its inclusion

---

## 🗓️ Roadmap

| Phase | Capabilities | Status |
|---|---|---|
| **v1.0** | All 5 user stories (this repo) | ✅ Shipped |
| v1.1 | AI-generated changelogs · Custom domain hosting · Per-tenant branding | Q4 2026 |
| v1.2 | Per-tenant SSO (SAML / Okta / Azure AD) + SCIM · Self-hosted deployment | Q1 2027 |
| v2.0 | AsyncAPI / gRPC / GraphQL support · Semantic diff · Streaming Try It | Q2 2027 |

**Explicitly out of scope for v1:** non-OpenAPI specs, semantic diff, on-prem, custom domains, enterprise SSO, AI changelogs.

---

## 🤝 Contributing

DocSynth follows a strict spec-driven workflow:

```
/speckit.specify  →  /speckit.clarify  →  /speckit.plan  →  /speckit.tasks  →  /speckit.implement
```

Every change starts with a spec update in `specs/<feature>/spec.md`. Architecture rules are enforced by ArchUnit tests in CI. AI-generated outputs are gated by eval thresholds (`specs/evals/thresholds.yml`).

See `.specify/memory/constitution.md` for the binding principles and the [contribution guide](.specify/) for tooling.

---

## 📄 License

[License TBD — internal/private at this time]

## 📞 Contact

- **Engineering:** [engineering@docsynth.dev](mailto:engineering@docsynth.dev)
- **Design partners:** [partners@docsynth.dev](mailto:partners@docsynth.dev)
- **Issues:** [GitHub Issues](../../issues)

---

<div align="center">

**DocSynth · v1.0 · 147 tasks shipped · 5 user stories · 0 [NEEDS CLARIFICATION] markers**

*The spec is the source of truth. The docs are derived. They stay in sync forever.*

</div>
