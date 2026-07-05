# PRD 20 — DocSynth

> **AI API documentation generator that stays in sync with your codebase.**

| | |
|---|---|
| **Product ID** | MSP-20 |
| **Category** | Developer Tools |
| **Type** | AI-Powered Micro SaaS |
| **Complexity** | Intermediate |
| **Methodology** | Spec-Driven Development (SDD) |
| **Primary stack** | SpringBoot Java + Python AI services |
| **Status** | Draft v1.0 |
| **Owner** | Solution Architect (portfolio: Raymund) |
| **Last updated** | 2026-06-24 |


## 1. Coverage Map

This PRD is written for production-grade delivery. Each required focus area maps to a section:

| Focus area | Where addressed |
| --- | --- |
| Business Requirements | Section 2 |
| System Design | Section 5.1 |
| Clean Architecture | Section 5.2 |
| Domain-Driven Design (DDD) | Section 5.3 |
| CQRS | Section 5.4 |
| CQRS Command Bus | Section 5.5 |
| Design Patterns | Section 5.6 |
| Event-Driven Architecture | Section 5.7 |
| Integration Patterns | Section 5.8 |
| Database Design | Section 6 |
| AI: LLMs / RAG / Agents / MCP / Vector DB / Memory / MLOps | Section 9 — AI Architecture |
| Web APIs | Section 10 |
| Performance Optimization | Section 12 |
| Security | Section 11 |
| Docker / Kubernetes / Cloud | Section 14 |
| CI/CD | Section 14.4 |
| Monitoring & Logging | Section 15 |
| Cost Optimization | Section 16 |
| Cross-Team Collaboration | Section 17 |
| Goals / Definition of Done | Section 20 |

## 2. Business Requirements

### 2.1 Problem
API docs rot: teams ship changes faster than they document, leaving inaccurate, distrusted docs.

### 2.2 Why now (2026)
2026 code-aware LLMs + CI hooks can regenerate accurate docs and examples on every merge.

### 2.3 Target users & personas
- Backend/platform engineer
- Developer-relations/docs owner
- API product manager

### 2.4 Value proposition
Always-current API docs, examples, and changelogs generated from code and specs on every merge.

### 2.5 Differentiator
Drift detection ties docs to code so they never silently go stale.

### 2.6 Business goals
1. Build working software that ships to real users, not a demo.
2. Build scalable software that grows from first customer to thousands of tenants.
3. Help teams deliver predictably via Spec-Driven Development and CI/CD.
4. Solve a real business problem: aPI docs rot: teams ship changes faster than they document, leaving inaccurate, distrusted docs.
5. Build a system that learns — improving from feedback, evaluations, and usage over time.

### 2.7 Success metrics (KPIs)
- Docs freshness (lag vs merge)
- Drift incidents caught
- Time-to-first-call (DX)
- Doc coverage %

### 2.8 Monetization
Per-repo/per-API subscription with usage tiers. This aligns with the 2026 shift to usage-based and hybrid pricing as autonomous features do measurable work.

### 2.9 Representative user stories
- As a **backend/platform engineer**, I want **openapi/code introspection** so that I get measurable value with less manual effort.
- As a **backend/platform engineer**, I want **endpoint reference + examples generation** so that I get measurable value with less manual effort.
- As a **backend/platform engineer**, I want **sdk snippet generation** so that I get measurable value with less manual effort.
- As a **backend/platform engineer**, I want **drift detection vs code** so that I get measurable value with less manual effort.
- As a **backend/platform engineer**, I want **interactive try-it console** so that I get measurable value with less manual effort.
- As a **backend/platform engineer**, I want **versioned docs** so that I get measurable value with less manual effort.


## 3. Product Scope

### 3.1 In scope (MVP)
- OpenAPI/code introspection
- Endpoint reference + examples generation
- SDK snippet generation
- Drift detection vs code
- Interactive try-it console
- Versioned docs
- CI/CD integration

### 3.2 Out of scope (initial release)
- Native mobile apps beyond a responsive/PWA client (phase 2 unless noted).
- On-prem self-hosting in the MVP (cloud-first; revisit for enterprise).
- Languages/locales beyond the launch set (i18n-ready, not fully localized at MVP).

### 3.3 Build emphasis (engineering scope)
This product is a vehicle to demonstrate: **Web APIs, Scalable Applications, Integration Patterns, Intelligent System, End to End Solutions**. Across the portfolio it also exercises CRUD, Web APIs, database design, scalable applications, microservices, distributed systems, end-to-end solution architecture, cross-team collaboration, and intelligent & autonomous systems.


## 4. Spec-Driven Development (SDD) Plan

This product is built **spec-first**: an executable specification — not ad-hoc prompting — is the source of truth, following the 2026 SDD practice popularized by GitHub Spec Kit, AWS Kiro, and the BMAD method. Tests, code, and docs are generated from and validated against the spec.

### 4.1 Spec artifacts (repo: `/spec`)
| Artifact | Purpose |
|---|---|
| `spec.md` | Intent, scope, personas, business rules, NFRs, constraints (this PRD distilled). |
| `plan.md` | Architecture decisions, bounded contexts, tech choices, milestone plan. |
| `tasks.md` | Decomposed, agent-executable tasks with acceptance criteria and traceability IDs. |
| `contracts/` | OpenAPI + event schemas + MCP tool schemas — the machine-readable contracts. |
| `evals/` | AI evaluation datasets and thresholds. |

### 4.2 SDD lifecycle
1. **Define intent** — capture the business problem and outcomes (Section 2).
2. **Remove ambiguity** — encode business rules and NFRs as testable statements.
3. **Plan with constraints** — Clean Architecture, DDD boundaries, NFRs (Sections 5–6).
4. **Implement with agents under oversight** — generate code/tests against `tasks.md`.
5. **Validate against the spec** — acceptance tests + AI evals gate every change in CI.

### 4.3 Sample acceptance criteria (executable specs)
- GIVEN a valid request to `IngestSpec` WHEN processed THEN the corresponding aggregate state changes and a `SpecIngested` event is published.
- GIVEN insufficient permissions WHEN any command is issued THEN the API returns 403 and no state changes.
- GIVEN a `GetDocSet` request THEN results are returned within the performance budget (Section 12) and respect tenant isolation.
- GIVEN an AI-generated output THEN it includes grounding/citations where applicable and passes the evaluation guardrails (Section 9.7) before being surfaced.

### 4.4 Traceability
Every requirement has an ID (`REQ-MSP20-n`) referenced by tasks, code, tests, and eval cases, so coverage is auditable end to end.

### 4.5 Agent Skills Registry

Development agents use installed skills from [skills.sh](https://www.skills.sh/) for domain-specific procedural knowledge. Each skill maps to a technology or practice in the stack:

| Domain | Skill | Installation | Purpose |
|--------|-------|-------------|---------|
| **Auth** | `auth0-springboot-api` | `npx skills add auth0/agent-skills --skill auth0-springboot-api` | Spring Boot API security with OIDC/OAuth2 |
| **Auth** | `auth0-vue` | `npx skills add auth0/agent-skills --skill auth0-vue` | Vue frontend authentication flows |
| **CI/CD** | `github-actions-docs` | `npx skills add xixu-me/skills --skill github-actions-docs` | Workflow syntax, runners, deployment patterns |
| **Database** | `supabase-postgres-best-practices` | `npx skills add supabase/agent-skills --skill supabase-postgres-best-practices` | Query tuning, indexing, RLS, connection pooling |
| **Cache** | `redis-development` | `npx skills add redis/agent-skills --skill redis-development` | Redis usage patterns and caching strategies |
| **Infra** | `azure-kubernetes` | `npx skills add microsoft/azure-skills --skill azure-kubernetes` | AKS cluster management and deployment |
| **Infra** | `azure-messaging` | `npx skills add microsoft/azure-skills --skill azure-messaging` | Service Bus / message queue configuration |
| **Infra** | `terraform-style-guide` | `npx skills add hashicorp/agent-skills --skill terraform-style-guide` | Terraform conventions and module structure |
| **Testing** | `tdd` | `npx skills add mattpocock/skills --skill tdd` | Red-Green-Refactor cycle, vertical slicing |
| **Workflow** | `writing-plans` | `npx skills add obra/superpowers --skill writing-plans` | Agent plan writing methodology |
| **Workflow** | `handoff` | `npx skills add mattpocock/skills --skill handoff` | Agent handoff and context passing |


## 5. System Design & Architecture

### 5.1 High-level system design
DocSynth is a **cloud-native, multi-tenant** system decomposed along bounded contexts. A front-end (responsive web/PWA) talks to an **API Gateway / BFF**, which routes to context-aligned Java/Spring services. A Python AI service hosts inference, RAG, and agents and is integrated through stable domain ports and MCP. Services communicate synchronously via REST/gRPC and asynchronously via a message bus using **integration events**. State changes are persisted transactionally and published reliably via the **outbox pattern**.

**Logical services / components**

| Service / component | Responsibility |
| --- | --- |
| Source Introspection Service | Owns the Source Introspection bounded context; exposes APIs and emits domain events. |
| Doc Generation Service | Owns the Doc Generation bounded context; exposes APIs and emits domain events. |
| Drift Detection Service | Owns the Drift Detection bounded context; exposes APIs and emits domain events. |
| Publishing Service | Owns the Publishing bounded context; exposes APIs and emits domain events. |
| Versioning Service | Owns the Versioning bounded context; exposes APIs and emits domain events. |
| AI/Inference Service (Python) | Hosts LLM orchestration, RAG, agents, and model serving; called via internal API/gRPC and MCP. |
| API Gateway / BFF | AuthN/Z, rate limiting, request routing, aggregation for the front-end. |

This satisfies the build goals of *microservices*, *distributed systems*, *scalable applications*, and *end-to-end solution architecture*.

### 5.2 Clean Architecture
The codebase follows Clean Architecture with strict dependency rules (dependencies point inward):

- **Domain** — entities, value objects, aggregates, domain events, and business rules. No framework dependencies.
- **Application** — use cases as CQRS commands/queries, command handlers, ports (interfaces), DTOs, validators.
- **Infrastructure** — JPA/Hibernate, message bus, caching, external/AI adapters implementing the ports.
- **Presentation (API)** — Spring Boot REST controllers, gateway, authentication.

Tooling enforces boundaries (ArchUnit / module layering tests) so the architecture cannot silently erode.

### 5.3 Domain-Driven Design (DDD)
**Bounded contexts:** Source Introspection, Doc Generation, Drift Detection, Publishing, Versioning.

**Aggregates & entities**

| Aggregate | Responsibility |
| --- | --- |
| ApiSpec | parsed schema |
| DocSet | generated reference |
| Example | code snippet |
| DriftReport | code-vs-doc diff |
| Project | repo/API tenant |

**Ubiquitous language (selected terms):** ApiSpec, Doc Generation, DocSet, Drift Detection, DriftReport, Example, Project, Publishing, Source Introspection, Versioning.

Context boundaries become service and module boundaries; a context map documents upstream/downstream relationships and where Anti-Corruption Layers protect the domain from external models.

### 5.4 CQRS
Commands and queries are separated. Commands enforce invariants on aggregates and emit events; queries read from denormalized, cache-friendly read models (and, where load demands, a separate read store).

**Commands**

| Command | Type | Behavior |
| --- | --- | --- |
| IngestSpec | Command | Mutates state in the Source Introspection context; validated, handled, emits event(s). |
| GenerateDocs | Command | Mutates state in the Doc Generation context; validated, handled, emits event(s). |
| DetectDrift | Command | Mutates state in the Drift Detection context; validated, handled, emits event(s). |
| PublishDocs | Command | Mutates state in the Publishing context; validated, handled, emits event(s). |
| TagVersion | Command | Mutates state in the Versioning context; validated, handled, emits event(s). |

**Queries**

| Query | Type | Behavior |
| --- | --- | --- |
| GetDocSet | Query | Reads from optimized read model; no side effects; cacheable. |
| GetDriftReport | Query | Reads from optimized read model; no side effects; cacheable. |
| ListVersions | Query | Reads from optimized read model; no side effects; cacheable. |
| GetCoverageMetrics | Query | Reads from optimized read model; no side effects; cacheable. |

### 5.5 CQRS Command Bus
A command bus (Spring Cloud Stream / Axon / custom mediator) mediates all application requests, keeping controllers thin and use cases isolated and testable.

**Representative handlers**
- `IngestSpecHandler` — validates, loads aggregate, applies behavior, persists, publishes event.
- `GenerateDocsHandler` — validates, loads aggregate, applies behavior, persists, publishes event.
- `DetectDriftHandler` — validates, loads aggregate, applies behavior, persists, publishes event.
- `PublishDocsHandler` — validates, loads aggregate, applies behavior, persists, publishes event.
- `TagVersionHandler` — validates, loads aggregate, applies behavior, persists, publishes event.

**Cross-cutting decorators:**
- `ValidationDecorator` — Jakarta Validation / Bean Validation on every request.
- `LoggingDecorator` — structured request/response logging with correlation IDs.
- `PerformanceDecorator` — flags slow handlers against the budget.
- `TransactionDecorator` — wraps commands in a unit of work + outbox.
- `CachingDecorator` — caches idempotent query results in Redis.
- `AiGuardrailDecorator` — applies prompt-injection, PII, and output-safety checks around AI calls.

### 5.6 Design Patterns
- **Mediator** (command bus) — decouples API controllers from application logic.
- **CQRS** — separate command and query models and, where useful, stores.
- **Repository + Unit of Work** — persistence abstraction over JPA/Hibernate.
- **Specification** — composable, testable query/business rules (Spring Data JPA Specifications).
- **Domain Events + Outbox** — reliable event publication with the transactional outbox pattern.
- **Factory / Builder** — construct complex aggregates and value objects.
- **Strategy** — pluggable algorithms (pricing, routing, scoring, ranking as applicable).
- **Decorator / Pipeline** — cross-cutting concerns (validation, logging, caching, retries).
- **Circuit Breaker + Retry (Resilience4j)** — resilient calls to external/AI services.
- **Saga / Process Manager** — coordinate multi-step, cross-service workflows.
- **Adapter / Anti-Corruption Layer** — isolate LLM/provider SDKs behind stable domain ports.

### 5.7 Event-Driven Architecture (EDA)
The system is event-driven internally and at its boundaries.

**Domain events**
- `SpecIngested` — domain event raised within a bounded context.
- `DocsGenerated` — domain event raised within a bounded context.
- `DriftDetected` — domain event raised within a bounded context.
- `DocsPublished` — domain event raised within a bounded context.
- `VersionTagged` — domain event raised within a bounded context.

**Integration events (published to the bus)**
- `SpecIngestedIntegrationEvent` — published to the bus for other services/consumers.
- `DocsGeneratedIntegrationEvent` — published to the bus for other services/consumers.
- `DriftDetectedIntegrationEvent` — published to the bus for other services/consumers.
- `DocsPublishedIntegrationEvent` — published to the bus for other services/consumers.

Events enable choreography between services, audit trails, and AI/ML feedback signals. Delivery uses the outbox pattern (exactly-once-effect), idempotent consumers, and a dead-letter queue for poison messages.

### 5.8 Integration Patterns
- **REST + OpenAPI** for synchronous external/internal APIs (versioned).
- **Async messaging** (integration events) for cross-service workflows and decoupling.
- **Webhooks** for inbound/outbound third-party event exchange (signed + idempotent).
- **Anti-Corruption Layer** wrapping each third-party integration: GitHub/GitLab CI, OpenAPI/Swagger, Static-site hosting, Slack alerts.
- **Model Context Protocol (MCP)** server/client to expose and consume tools for agents (Section 9.4).


## 6. Data & Database Design

### 6.1 Storage strategy
Primary operational store: **PostgreSQL + pgvector**. Reads use CQRS read models / materialized views; hot paths are cached in **Redis**. Each tenant's data is isolated (row-level security + tenant key on every table). Migrations are managed by Flyway/Liquibase and run automatically in CI/CD with safe, backward-compatible changes.

### 6.2 Core entities (selected)
| Table / entity | Concern | Notes |
| --- | --- | --- |
| ApiSpec | Write model (normalized) | parsed schema |
| DocSet | Write model (normalized) | generated reference |
| Example | Write model (normalized) | code snippet |
| DriftReport | Write model (normalized) | code-vs-doc diff |
| Project | Write model (normalized) | repo/API tenant |
| OutboxMessage | Reliability | Stores domain/integration events for transactional publication. |
| AuditLog | Compliance | Append-only record of security-relevant and state-changing actions. |
| Tenant | Multi-tenancy | Tenant registry; drives row-level isolation and routing. |

### 6.3 Vector & semantic store
Embeddings and semantic search use **pgvector over code + docs**. Chunked content is stored with rich metadata (source, ACL, timestamps, version) to support filtered, hybrid retrieval (Section 9.2).

### 6.4 Data lifecycle & governance
Retention policies per data class, soft-delete with purge windows, encryption at rest, PII tagging, and per-tenant export/delete to satisfy GDPR/CCPA. Backups are automated with tested point-in-time restore.


## 7. Tech Stack

The recommended stack is a **hybrid: SpringBoot Java core + Python AI services**, using Java/Spring where stability, performance, and enterprise patterns are paramount, and Python where the AI ecosystem is strongest.

| Layer | Choice |
| --- | --- |
| Language / runtime | Java 21 (SpringBoot 3.x) + Python 3.12 (AI services) |
| Web/API | Spring Boot (WebMVC/WebFlux, REST controllers), Spring Cloud |
| Persistence | PostgreSQL + pgvector via Spring Data JPA / Hibernate |
| Caching | Redis via Spring Cache / Lettuce |
| Messaging | Azure Service Bus / RabbitMQ (Spring Cloud Stream) + transactional outbox |
| Auth | OpenID Connect (Spring Security / Resource Server), RBAC/ABAC |
| Front-end | Vue 3 (Vite + Pinia + Vue Router), GitHub App integration |
| Containers/Orchestration | Docker + Kubernetes (AKS), Helm/Kustomize, Argo Rollouts |
| IaC | Terraform |
| CI/CD | GitHub Actions, cosign, SBOM |
| Observability | OpenTelemetry, Logback, Grafana/Azure Monitor |
| AI service | Python doc-gen service + parsers |
| LLM orchestration | Spring AI + LangChain/LlamaIndex (Python) |
| Vector / search | pgvector over code + docs |
| Model providers | Azure OpenAI / Anthropic / open models via an adapter; routing by cost & task |
| AI integration | MCP (server/client) + internal gRPC |
| MLOps/eval | Eval harness (Ragas-style), prompt/model registry, drift monitors |

## 8. Build Scope Mapping

This product especially showcases: **Web APIs, Scalable Applications, Integration Patterns, Intelligent System, End to End Solutions**.

| Build capability | How this product demonstrates it |
| --- | --- |
| CRUD Applications | Core entity management across the bounded contexts with validation and audit. |
| Web APIs | Versioned REST + OpenAPI; gRPC internally (Section 10). |
| Database Design | Normalized write models, CQRS read models, multi-tenant isolation (Section 6). |
| Scalable Applications | Stateless services + HPA + caching + async (Sections 12–14). |
| Microservices | Context-aligned services with independent deploy/scaling (Section 5.1). |
| Distributed Systems | Async messaging, outbox, sagas, idempotency, resilience (Sections 5.7, 13). |
| End to End Solutions | Front-end → API → domain → data → infra → CI/CD, fully delivered. |
| Solution Architect | Documented architecture, ADRs, context map, NFRs, and trade-offs. |
| Cross Team Collaboration | Contracts-first parallel delivery (Section 17). |
| Intelligent System | Grounded LLM/RAG features that adapt to data and feedback (Section 9). |
| Autonomous Solution | Agents that plan and act via tools/MCP with human-in-the-loop guardrails (Sections 9.3–9.4). |

## 9. AI Architecture

> Principle for this portfolio: **AI amplifies software engineering, it does not replace it.** DocSynth is a production-grade intelligent system, not a demo. *Better context beats bigger models.*


### 9.1 LLMs
Code-aware LLM for docs + examples. Models are accessed behind a provider-agnostic **Adapter/ACL** so we can route by task, cost, and latency, and fail over between providers. Prompt templates are versioned in `/spec/contracts`. Token budgets, max-context windows, and temperature are configured per use case. A small/cheap model handles routing, extraction, and classification; a frontier model handles complex generation.

### 9.2 RAG (Retrieval-Augmented Generation)
RAG is treated as **the product**, not a feature — an ecosystem of interconnected layers. Product-specific role: Retrieve codebase + prior docs for grounding.

- **Query construction** — transform user intent into searchable context; combine relational, graph, and vector signals to improve precision.
- **Routing** — logical + semantic routing to the right knowledge source to cut unnecessary retrieval cost.
- **Indexing** — semantic chunking, multi-representation indexing, hierarchical indexing (RAPTOR), and advanced embeddings (hybrid / ColBERT-style late interaction).
- **Retrieval** — multi-stage pipeline with query refinement, re-ranking, and context optimization before generation.
- **Generation** — retrieval-aware prompting, active context selection, grounded answers with citations.
- **Evaluation** — measure retrieval quality and answer relevance/faithfulness, benchmark performance, and continuously improve (Section 9.7).

The competitive edge comes from knowledge quality, retrieval accuracy, context relevance, and the evaluation framework — not model choice alone.

### 9.3 AI Agents
CI agent that regenerates and validates docs. The agent layer implements **tool calling**, **planning & reasoning**, **memory**, and (where useful) **multi-agent** collaboration. Tools are typed, permissioned, and observable; every tool call is logged with inputs/outputs for audit and evaluation. Agent autonomy is bounded by policies and human-in-the-loop checkpoints for high-impact actions.

### 9.4 MCP (Model Context Protocol)
MCP tools for repo + publish MCP is the 2026 standard integration layer (adopted across major AI platforms), which lowers integration cost and makes capabilities reusable across agents.

**MCP tools (server surface)**

| Tool | Description |
| --- | --- |
| `parse_spec` | Tool exposed/consumed via MCP for agent use. |
| `generate_docs` | Tool exposed/consumed via MCP for agent use. |
| `detect_drift` | Tool exposed/consumed via MCP for agent use. |
| `publish_docs` | Tool exposed/consumed via MCP for agent use. |

Tools are schema-defined, authorized per tenant/scope, rate-limited, and audited.

### 9.5 Vector Databases
**pgvector over code + docs** stores embeddings with metadata for filtered, hybrid (keyword + vector) search and re-ranking. Index lifecycle (build, refresh, compaction), embedding versioning, and backfills are automated. Retrieval respects tenant isolation and document-level ACLs.

### 9.6 AI Memory Systems
Durable cross-session memory is **not core** to this product (work is largely stateless per request), so the MVP keeps only **short-term working memory** within a single task/run. The architecture still leaves a clean seam for it: a governed memory store (episodic + semantic) can be added behind the same retrieval layer if usage shows value in remembering prior context — without changing the domain or API surface.

### 9.7 MLOps & Production AI
Doc-accuracy eval, example-compile checks in CI

- **Data pipelines** — ingestion, cleaning, chunking, and embedding jobs are versioned and reproducible.
- **Model/prompt registry** — versioned prompts, models, and configs with staged rollout.
- **Evaluation** — automated eval sets for relevance, faithfulness/grounding, and task success run in CI; no AI change ships without passing thresholds.
- **Guardrails** — prompt-injection defense, PII redaction, output moderation, and grounding checks (the `AiGuardrailBehavior`).
- **Observability** — trace every LLM/agent/tool call (tokens, cost, latency, outcome); see Section 15.
- **Drift detection** — monitor input/output distributions and quality KPIs; alert and trigger re-index/re-tune.
- **Human feedback loops** — capture accept/reject/edit signals to improve retrieval, prompts, and (where justified) fine-tuning.
- **Reliability & cost** — caching, batching, fallbacks, and budget caps (Sections 12 & 16).

## 10. Web API Design
RESTful, versioned (`/api/v1`), documented with OpenAPI (auto-published via the docs pipeline). JSON over HTTPS, cursor pagination, RFC 7807 problem-details errors, idempotency keys on commands, ETags on resources, and consistent rate-limit headers. An internal gRPC contract connects Java/Spring services to the Python AI service for low-latency inference.

**Representative endpoints**

| Method | Path | Kind | Notes |
| --- | --- | --- | --- |
| POST | /api/v1/ingestSpec | Command | Auth required; validated; idempotency-key supported. |
| POST | /api/v1/generateDocs | Command | Auth required; validated; idempotency-key supported. |
| POST | /api/v1/detectDrift | Command | Auth required; validated; idempotency-key supported. |
| POST | /api/v1/publishDocs | Command | Auth required; validated; idempotency-key supported. |
| POST | /api/v1/tagVersion | Command | Auth required; validated; idempotency-key supported. |
| GET | /api/v1/docset | Query | Auth required; cacheable; paginated. |
| GET | /api/v1/driftreport | Query | Auth required; cacheable; paginated. |
| GET | /api/v1/versions | Query | Auth required; cacheable; paginated. |
| GET | /api/v1/coveragemetrics | Query | Auth required; cacheable; paginated. |

Webhooks (signed, versioned, retried) let customers subscribe to events such as `SpecIngested`. An MCP server exposes the same capabilities to AI agents (Section 9.4).


## 11. Security
Security is designed in from day one (SOC 2 Type II and GDPR/CCPA readiness).

- **AuthN** — OIDC/OAuth2 (e.g., Microsoft Entra ID / Auth0); SSO + SCIM for enterprise; MFA.
- **AuthZ** — role- and attribute-based access control; per-tenant authorization enforced in the application layer.
- **Multi-tenant isolation** — tenant key on every row + row-level security; no cross-tenant data access by construction.
- **Secrets** — managed vault (Azure Key Vault / AWS Secrets Manager); no secrets in code or images.
- **Data protection** — TLS 1.2+ in transit, AES-256 at rest, field-level encryption for sensitive data, PII tagging.
- **API security** — input validation, output encoding, rate limiting, WAF, OWASP API Top 10 controls.
- **Auditability** — append-only audit log of security-relevant actions; tamper-evident.
- **Supply chain** — SCA/SAST/secret scanning and signed images in CI/CD; SBOM generated per build.
- **Prompt-injection & jailbreak defense** — input/output filtering, tool-permission scoping, and content provenance.
- **Data governance for AI** — retrieval respects ACLs; no training on customer data without explicit consent; PII redaction before model calls.
- **Output safety** — grounding/citation checks and moderation before AI output is shown or acted upon.


## 12. Performance Optimization
**Budgets:** API reads p95 < 200 ms, writes p95 < 400 ms, AI responses streamed with first-token < 1.5 s and grounded answer < 6 s.

- Multi-layer caching (Redis + HTTP/CDN) for hot queries; cache-aside with invalidation on events.
- CQRS read models / materialized views to avoid expensive joins on hot paths.
- Async, non-blocking I/O; bulk/batch operations; connection pooling; pagination everywhere.
- Database indexing strategy reviewed per query; N+1 prevention; query plans monitored.
- Back-pressure and queue-based load leveling for spiky/expensive work (including AI inference).
- AI-specific: prompt/result caching, embedding caching, response streaming, model routing (small model first), and batching of embeddings.


## 13. Scalability & Reliability
- **Stateless services** scale horizontally behind the gateway; sticky state externalized to data/cache.
- **Async workers** scale independently for inference, ingestion, and background and scheduled work.
- **Resilience** — Resilience4j retries with jittered backoff, circuit breakers, timeouts, and bulkheads around external/AI dependencies.
- **Reliability** — outbox + idempotent consumers for exactly-once effects; dead-letter queues; sagas for multi-step consistency.
- **Targets** — 99.9% API availability; graceful degradation (serve cached/looked-up answers when the model is unavailable).
- **Autoscaling** — Kubernetes HPA on CPU/RPS/queue depth and GPU/inference concurrency for the AI service.


## 14. Infrastructure & DevOps

### 14.1 Docker
Every service ships as a small, multi-stage Docker image (distroless/Alpine base, non-root user, pinned digests). `docker-compose` provides a one-command local environment (services + Postgres + Redis + vector store + a stub AI service).

### 14.2 Kubernetes
Deployed to managed Kubernetes (AKS primary). Each service has Deployments, Services, HPAs, readiness/liveness probes, resource requests/limits, PodDisruptionBudgets, and network policies. Config via ConfigMaps/Secrets (CSI driver to the vault). Ingress via NGINX/Gateway API with TLS from cert-manager. GPU node pool (with scale-to-zero) hosts the Python AI service; KEDA scales workers on queue depth. Helm/Kustomize manage manifests; progressive delivery via Argo Rollouts (canary/blue-green).

### 14.3 Cloud
Cloud-first on **Azure** (portfolio default): AKS, Azure Database for PostgreSQL, Azure Cache for Redis, Service Bus, Blob Storage, Key Vault, Azure AI Search, and Azure OpenAI/serverless GPU for inference. Infrastructure as Code with **Terraform**; environments (dev/staging/prod) are reproducible and isolated. The design is cloud-portable (AWS/GCP equivalents documented).

### 14.4 CI/CD
GitHub Actions (or Azure DevOps) pipelines: restore → build → unit/integration tests → AI evals → SAST/SCA/secret-scan → container build + sign (cosign) → push → deploy to staging → smoke/contract tests → progressive prod rollout. Trunk-based development, PR checks (including ReviewMate-style automated review), and IaC plan/apply gates. Database migrations run automatically with backward-compatible, expand-contract changes. Rollbacks are automated on failed health/eval gates.


## 15. Monitoring & Logging
Full observability via **OpenTelemetry** (traces, metrics, logs) exported to a backend (Azure Monitor / Grafana stack / Datadog).

- **Structured logging** with correlation/trace IDs across services (Logback in Java).
- **File-based logging** — every service writes daily-rotating JSON logs to `logs/<service>/<YYYY-MM-DD>.json` using `logstash-logback-encoder`. Each entry is a single JSON line containing: timestamp, level, service name, trace/span IDs, tenant ID, user ID, thread, logger, message, stack trace (if error), and any custom MDC fields. Format:
  ```json
  {"@timestamp":"2026-07-01T10:30:00.123Z","level":"INFO","service":"source-introspection","traceId":"abc123","spanId":"def456","tenantId":"tnt-001","userId":"usr-042","logger":"c.d.s.IntrospectionService","message":"Spec ingested","mdc":{"specId":"spec-009","source":"github"},"exception":null}
  ```
  Retention: 30 days on disk, then compressed archive for 90 days, then purged. Logback config enforces a size cap (500 MB per day per service) and a total disk quota (5 GB per service) as a safety net.
- **Metrics** — RED/USE dashboards: rate, errors, duration, saturation per service; business KPIs from Section 2.7.
- **Tracing** — distributed traces across gateway → services → data → AI service → model/tool calls.
- **Alerting** — SLO-based alerts (error budgets), on-call routing (PagerDuty/Opsgenie), and runbooks.
- **Audit & compliance logs** retained per policy.
- **AI observability** — per-call token usage, cost, latency, retrieval hits, grounding/eval scores, and drift metrics; sampled traces of prompts/outputs (PII-scrubbed).


## 16. Cost Optimization
Cost is a first-class architectural concern (it protects the unit economics of a micro SaaS).

- Right-sized Kubernetes requests/limits; cluster autoscaler + scale-to-zero for non-prod and bursty workers.
- Spot/low-priority nodes for fault-tolerant batch jobs; reserved/savings plans for steady baseline.
- Caching and CQRS read models to cut database load; storage tiering and lifecycle policies for cold data.
- FinOps: per-tenant cost attribution and dashboards tied to usage-based pricing so margins are visible.
- Budget alerts and anomaly detection on cloud spend.
- Model routing (cheap model first), prompt/response and embedding caching, batching, and max-token caps.
- Retrieval tuning to send only the most relevant context (fewer tokens = lower cost and better answers).
- GPU scale-to-zero and serverless inference for spiky AI load; per-tenant token budgets and rate limits.


## 17. Cross-Team Collaboration & Delivery
- **Contracts as the interface** — OpenAPI, event schemas, MCP tool schemas, and `spec.md` let front-end, back-end, AI, and platform teams work in parallel against agreed boundaries.
- **DDD context map** assigns clear ownership per bounded context, reducing cross-team coupling.
- **Spec-Driven Development** gives a shared, executable source of truth; tasks are decomposed with acceptance criteria so work parallelizes cleanly.
- **CI/CD + trunk-based development** keep integration continuous; feature flags (Flagpole-style) decouple deploy from release.
- **Definition of Ready/Done**, ADRs (architecture decision records), and runbooks keep teams aligned and onboarding fast.


## 18. Roadmap & Milestones

| Phase | Outcomes |
| --- | --- |
| Phase 0 — Spec & foundations (2–3 wks) | Author `spec.md`/`plan.md`/`tasks.md`, set up repo, CI/CD skeleton, Docker/K8s base, auth, multi-tenancy. |
| Phase 1 — Core MVP (4–6 wks) | Implement core bounded contexts (Source Introspection, Doc Generation…), CQRS commands/queries, primary CRUD + APIs, and the top features. |
| Phase 2 — Intelligence (4–6 wks) | Stand up the Python AI service: LLM orchestration, RAG, agents/MCP, vector store, and the evaluation harness. |
| Phase 3 — Production hardening (3–4 wks) | Security review, performance/load testing, observability, cost tuning, and progressive rollout to first customers. |
| Phase 4 — Learn & expand | Close feedback/eval loops, tune retrieval & prompts, expand autonomy and integrations. |

## 19. Risks & Mitigations

| Risk | Severity | Mitigation |
| --- | --- | --- |
| Scope creep on the MVP | Medium | SDD spec + ruthless out-of-scope list; ship the thin end-to-end slice first. |
| Multi-tenant data leakage | High | Row-level security, tenant key everywhere, automated isolation tests in CI. |
| Third-party API changes/limits | Medium | Anti-Corruption Layer, contract tests, retries/circuit breakers, vendor fallbacks. |
| Cloud cost overruns | Medium | FinOps dashboards, budgets/alerts, autoscaling, caching (Section 16). |
| Hallucination / wrong AI output | High | Grounded RAG with citations, eval thresholds in CI, guardrails, human-in-the-loop on high-impact actions. |
| Model/provider cost or outage | Medium | Model routing + caching, provider fallback via the adapter, budget caps. |
| Prompt injection / data exfiltration | High | Input/output filtering, scoped tool permissions, ACL-aware retrieval, audit of tool calls. |

## 20. Goals & Definition of Done

### 20.1 How this product delivers the portfolio goals
| Goal | How it is achieved |
| --- | --- |
| Build working software | Thin end-to-end vertical slice shipped in Phase 1; everything is deployable and tested from day one. |
| Build scalable software | Stateless services + Kubernetes HPA, CQRS read models, async messaging, multi-tenant by design (Sections 13–14). |
| Help teams deliver | Spec-Driven Development, contracts-first parallelism, CI/CD, and clear DDD ownership (Sections 4 & 17). |
| Solve a real business problem | Directly targets: aPI docs rot: teams ship changes faster than they document, leaving inaccurate, distrusted docs. — measured by the KPIs in Section 2.7. |
| Build a system that learns | Evaluation harness, human-feedback loops, drift detection, memory, and continuous retrieval/prompt tuning (Section 9.7). |

### 20.2 Definition of Done
- All acceptance criteria (Section 4.3) pass in CI.
- Security checks, SAST/SCA, and tenant-isolation tests pass.
- Performance budgets (Section 12) met under load test.
- Observability dashboards and alerts live (Section 15).
- Docs/OpenAPI published and runbooks written.
- AI evaluation thresholds (relevance/faithfulness/task success) met (Section 9.7).

---
*Generated for the 2026 Micro SaaS Portfolio — built Spec-First. AI amplifies software engineering; it does not replace it.*

