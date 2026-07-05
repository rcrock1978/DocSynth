# Implementation Plan: DocSynth Core — AI API Documentation Generator

**Branch**: `001-docsynth-core` | **Date**: 2026-07-03 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-docsynth-core/spec.md`

## Summary

Build DocSynth, a multi-tenant SaaS that ingests OpenAPI 3.x specifications (from URL, file, or GitHub repo), generates versioned human-readable reference docs with code examples, detects structural drift across spec versions, exposes an interactive "Try It" API console, and publishes immutable versioned doc sets at stable URLs. The system must serve 100 projects/tenant and 500 endpoints/project at a target 99.5% monthly uptime with RPO ≤ 1h and RTO ≤ 4h.

Technical approach: **Java 21 / Spring Boot 3.x backend** orchestrates AI work and persists data in **PostgreSQL + pgvector**; a **Python 3.12 sidecar** runs LangChain/LlamaIndex for AI-bound work, called via **gRPC** with JWT-asserted tenant context. A **Vue 3 (Vite + Pinia) frontend** is built twice — once as a static SSG bundle per versioned DocSet (immutable, content-hashed, CDN-served) and once as the dynamic operator UI for ingestion/publish/console management. **Azure Service Bus** carries asynchronous drift-detection work via a transactional outbox. The **Try It console** uses a tenant-scoped, allowlist-validated server-side proxy with the security mitigations enumerated in `research.md §5`.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3.x), Python 3.12, TypeScript 5.x (Vue 3 + Vite)

**Primary Dependencies**:
- Backend: Spring Boot 3.x, Spring Cloud, Spring Data JPA/Hibernate, Spring Security (OIDC/OAuth2), Spring AI, Spring Cloud Stream, Resilience4j
- Persistence: PostgreSQL 16 + pgvector, Flyway
- AI sidecar: Python 3.12, LangChain 0.3.x, LlamaIndex 0.12–0.13.x, gRPC Python
- Inter-service: gRPC (primary), REST (admin/health)
- OpenAPI tooling: `org.openapitools.openapidiff:openapi-diff-core` (drift detection), `io.swagger.parser.v3:swagger-parser` (parsing)
- Frontend: Vue 3, Vite (with Vite SSG for published docs), Pinia, Vue Router
- Messaging: Azure Service Bus (Spring Cloud Stream binder), transactional outbox
- Observability: Micrometer → OpenTelemetry → Azure Monitor / Prometheus / Grafana; structured JSON logs (Logback ECS encoder)
- Testing: JUnit 5, Testcontainers (Postgres/Service Bus/Key Vault), ArchUnit, Playwright (frontend E2E), pytest (AI sidecar)

**Storage**:
- Operational data: PostgreSQL with row-level security (tenant_id on every table)
- Vector store: pgvector (same Postgres, separate schema)
- Published DocSets: object storage (Azure Blob) with version-segmented, content-hashed, immutable prefixes
- Secrets: managed secret store (Azure Key Vault)

**Testing**: JUnit 5 + Testcontainers (unit/integration), ArchUnit (architecture rules), Playwright (Vue E2E), pytest (AI sidecar), Pact (consumer-driven contract tests for gRPC). AI eval harness in `specs/evals/` per Constitution Principle III.

**Target Platform**: Linux containers on AKS (Azure Kubernetes Service); deployed via Terraform + GitHub Actions; CDN via Azure Front Door.

**Project Type**: Web application (frontend + backend + Python AI service)

**Performance Goals** (from spec):
- SC-001: 50-endpoint spec ingestion ≤ 30 s
- SC-002: doc generation ≤ 60 s
- SC-003: drift detection ≤ 2 min
- SC-004: Try It response ≤ 5 s
- SC-005: ≥ 95% user success rate (no support)
- SC-006: 100 projects/tenant × 500 endpoints/project without degradation
- SC-007: 99.5% monthly uptime, RPO ≤ 1 h, RTO ≤ 4 h

**Constraints**:
- Multi-tenant SaaS only; no on-prem / self-hosted in v1
- OpenAPI 3.x only; other spec formats deferred to future releases
- Java/Spring is the orchestration plane; Python is the model plane
- Tenant isolation enforced at data layer (RLS) with per-request tenant context
- No secrets in logs or API responses (FR-011)
- All operations audited (FR-009, FR-013)

**Scale/Scope** (MVP):
- 100 projects/tenant, 500 endpoints/project (SC-006)
- Up to ~50k endpoints/tenant at MVP ceiling
- Drift reports stored per comparison; retention policy to be defined in plan
- CDN-cached doc reads scale horizontally without backend involvement

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|---|---|---|
| **I. Spec-Driven Development** | ✅ PASS | `spec.md` is the source of truth; this plan and downstream `tasks.md` trace back to FR-001..FR-014. |
| **II. Clean Architecture & DDD** | ✅ PASS | Dependency rules enforced: Domain → Application → Infrastructure → Presentation. Bounded contexts: `ingestion`, `documentation`, `drift`, `publishing`, `identity`. ArchUnit tests gate the build. |
| **III. Test-First with AI Evaluation** | ✅ PASS | Acceptance criteria written in spec; TDD cycle per story. AI-generated outputs (code examples, descriptions) MUST pass the eval harness in `specs/evals/` (relevance, faithfulness, task success) before reaching users. Eval thresholds versioned and gated in CI. |
| **IV. Observability by Default** | ✅ PASS | FR-013 codifies structured JSON logs (90d), RED/USE metrics, distributed traces with correlation IDs. |
| **V. Simplicity & YAGNI** | ✅ PASS | No premature abstraction: SSG is simple, RLS is simple, gRPC is justified by streaming + schema enforcement. Complexity is documented inline (see Complexity Tracking — none at MVP). |

**Technology & Architecture Constraints** (from constitution): all adopted without exception.
- Backend: Java 21 / Spring Boot 3.x ✅
- Frontend: Vue 3 (Vite + Pinia) ✅
- AI: Python 3.12, LangChain/LlamaIndex, Spring AI orchestration ✅
- Database: PostgreSQL + pgvector ✅
- Messaging: Azure Service Bus via Spring Cloud Stream + transactional outbox ✅
- Infrastructure: Docker, AKS, Terraform, GitHub Actions ✅
- Multi-tenancy: row-level security + tenant key on every table ✅
- API: REST (versioned, OpenAPI-documented) + internal gRPC + MCP for agent tools ✅
- Security: OIDC/OAuth2, RBAC/ABAC, AES-256 at rest, TLS 1.2+ in transit ✅

**Re-evaluation after Phase 1 design:** no new violations introduced. Constitution check passes.

## Project Structure

### Documentation (this feature)

```text
specs/001-docsynth-core/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output — 4 decisions documented
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output — gRPC proto + OpenAPI specs
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
backend/                          # Java 21 / Spring Boot 3.x
├── src/main/java/com/docsynth/
│   ├── domain/                   # Domain models, value objects, domain events
│   │   ├── ingestion/            # ApiSpec, SpecSource
│   │   ├── documentation/        # DocSet, DocSetVersion, DocSetState
│   │   ├── drift/                # DriftReport, ChangeKind
│   │   ├── project/              # Project, Tenant
│   │   └── audit/                # AuditEntry
│   ├── application/              # Use cases, application services, ports
│   │   ├── ingestion/            # IngestSpecUseCase, ParseSpecPort
│   │   ├── documentation/        # GenerateDocSetUseCase, PublishDocSetUseCase
│   │   ├── drift/                # DetectDriftUseCase, DiffSpecPort
│   │   └── proxy/                # TryItProxyUseCase, TargetAllowlistPolicy
│   ├── infrastructure/           # Adapters: JPA, gRPC clients, Service Bus, S3/Blob
│   │   ├── persistence/          # Spring Data JPA repositories, RLS filter
│   │   ├── messaging/            # Spring Cloud Stream producers/consumers
│   │   ├── grpc/                 # gRPC client to AI sidecar
│   │   ├── storage/              # Blob storage adapter (immutable prefix writes)
│   │   ├── ai/                   # Spring AI configuration; MCP server for tool exposure
│   │   └── security/             # OIDC config, RBAC, secret store client
│   ├── interfaces/               # Inbound adapters
│   │   ├── api/                  # REST controllers (operator UI)
│   │   ├── grpc/                 # gRPC servers (internal service-to-service)
│   │   └── proxy/                # Try It proxy endpoint
│   └── config/                   # Spring configuration
├── src/main/resources/
│   ├── db/migration/             # Flyway migrations
│   └── application.yml
└── src/test/                     # Unit, integration, ArchUnit, contract tests

ai-sidecar/                       # Python 3.12 — model-bound work
├── src/docsynth_ai/
│   ├── pipelines/                # LangChain / LlamaIndex pipelines
│   │   ├── code_examples.py      # Generate SDK snippets
│   │   ├── description_enhance.py
│   │   └── drift_narrate.py      # (v2) narrate drift reports
│   ├── retrieval/                # pgvector retrieval
│   ├── evals/                    # Eval harness runners
│   └── server.py                 # gRPC server entry point
├── tests/                        # pytest
└── pyproject.toml

frontend/                         # Vue 3 — two build targets
├── src/
│   ├── operator/                 # Dynamic UI: ingest, publish, manage, console config
│   │   ├── pages/
│   │   ├── components/
│   │   ├── stores/               # Pinia
│   │   └── router/
│   ├── docs/                     # Pre-rendered DocSet runtime (SSG'd per version)
│   │   ├── components/           # Endpoint reference, code examples, Try It island
│   │   └── pages/
│   └── shared/                   # Types, API clients (operator uses REST, docs uses proxy)
├── e2e/                          # Playwright
└── vite.config.ts                # Two build modes: operator (SPA) + docs (SSG)

infra/                            # Terraform
├── modules/
│   ├── aks/
│   ├── postgres/                 # With pgvector extension
│   ├── servicebus/
│   ├── keyvault/
│   ├── blobstorage/              # Immutable prefix policies
│   └── frontdoor/                # CDN
└── envs/
    ├── dev/
    ├── staging/
    └── prod/

contracts/                        # Cross-language contract definitions
├── proto/                        # gRPC service definitions
│   ├── ai_orchestration.proto
│   ├── ingestion.proto
│   └── drift.proto
└── openapi/                      # Operator REST API OpenAPI 3.x specs
    ├── operator-api.yaml
    └── proxy-api.yaml

specs/evals/                      # AI eval thresholds (Constitution Principle III)
├── code_examples/
├── description_enhance/
└── thresholds.yml

.github/workflows/                # CI: build → test → AI evals → SAST/SCA → container sign → deploy
```

**Structure Decision**: **Option 2 (web application)** with an additional Python service. The frontend is built twice from the same Vue codebase — once as an SSG bundle per DocSet (immutable, CDN-served) and once as the dynamic operator SPA. This honors the versioned publishing decision in `research.md` and avoids duplicating UI code in two repositories. The Python AI sidecar is a peer to the Java backend, not a sub-component, and gets its own module.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No constitution violations at MVP. The four research decisions (SSG-hybrid publishing, gRPC inter-service, openapi-diff-core for drift, validated proxy for Try It) are the simplest design that satisfies the spec and constitution, and are documented in `research.md`. No complexity tracking entries required.
