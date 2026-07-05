---
description: "Task list for DocSynth Core implementation"
---

# Tasks: DocSynth Core — AI API Documentation Generator

**Input**: Design documents from `/specs/001-docsynth-core/`
- `plan.md` (required) — tech stack, project structure, constitution check
- `spec.md` (required) — 5 user stories (P1, P1, P2, P3, P3) + 14 functional requirements
- `research.md` (available) — 4 architectural decisions (Try It proxy, drift detection, AI orchestration, SSG-hybrid publishing)
- `data-model.md` (available) — 14 entities with RLS
- `contracts/README.md` (available) — operator REST + gRPC + Try It proxy contracts
- `quickstart.md` (to be generated in Phase 1)

**Tests**: Spec does not explicitly request TDD, but the constitution requires Test-First with AI Evaluation (Principle III) and ArchUnit gates (Principle II). Each user-story phase includes the test tasks required by the constitution and the spec's acceptance scenarios.

**Organization**: Tasks grouped by user story for independent implementation and testing. Setup and Foundational phases block all stories.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4, US5)
- Include exact file paths

## Path Conventions

- Backend (Java/Spring): `backend/src/main/java/com/docsynth/...`, `backend/src/main/resources/`, `backend/src/test/...`
- AI sidecar (Python): `ai-sidecar/src/docsynth_ai/...`, `ai-sidecar/tests/...`
- Frontend (Vue 3): `frontend/src/operator/...`, `frontend/src/docs/...`, `frontend/e2e/...`
- Shared: `contracts/...`, `infra/...`, `.github/workflows/...`, `specs/evals/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize repositories, tooling, and shared infrastructure per `plan.md` structure.

- [X] T001 Create monorepo directory layout per `plan.md` §Project Structure: `backend/`, `ai-sidecar/`, `frontend/`, `contracts/`, `infra/`, `specs/evals/`, `.github/workflows/`
- [X] T002 [P] Initialize Spring Boot 3.x project (Java 21, Maven) in `backend/pom.xml` with dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, spring-cloud-stream-binder-servicebus, spring-ai-core, resilience4j-spring-boot3, postgresql driver, flyway-core, flyway-database-postgresql, micrometer-tracing-bridge-otel, opentelemetry-exporter-otlp
- [X] T003 [P] Initialize Python 3.12 project in `ai-sidecar/pyproject.toml` with: langchain 0.3.x, llama-index 0.12–0.13.x, grpcio, grpcio-tools, pydantic, pytest, mypy
- [X] T004 [P] Initialize Vue 3 + Vite + TypeScript project in `frontend/package.json` with: vue@3, vite, vite-plugin-ssg, pinia, vue-router, @vueuse/core, typescript, vitest, @playwright/test
- [X] T005 [P] Configure linting/formatting: Checkstyle + Spotless (Maven) for `backend/`, ruff + mypy for `ai-sidecar/`, ESLint + Prettier for `frontend/`
- [X] T006 [P] Add `ArchUnit` baseline test in `backend/src/test/java/com/docsynth/architecture/LayerDependencyTest.java` enforcing Domain → Application → Infrastructure → Presentation (Constitution Principle II)
- [X] T007 [P] Create `proto/` directory and add `protoc` build step in `contracts/proto/buf.gen.yaml`
- [X] T008 Create root `README.md` linking to `specs/001-docsynth-core/{plan,spec,research,data-model}.md` and `contracts/`
- [X] T009 [P] Add `.gitignore`, `.editorconfig`, `.nvmrc`, `.python-version`, `.sdkmanrc` (Java 21)
- [X] T010 [P] Initialize Terraform root in `infra/main.tf` with provider config for Azure (AKS, Postgres, Service Bus, Key Vault, Blob Storage, Front Door)
- [X] T011 [P] Create GitHub Actions workflow `.github/workflows/ci.yml` with: build, unit tests, integration tests, AI evals, SAST (CodeQL), SCA (Dependabot), container build + sign (cosign), terraform plan
- [X] T012 Generate `specs/001-docsynth-core/quickstart.md` per `plan.md` design (prerequisites, setup commands, runnable validation scenarios for each user story, expected outcomes)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story. All user stories depend on this phase.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T013 Provision Azure resources per `infra/`: AKS cluster, PostgreSQL Flexible Server with pgvector extension, Service Bus namespace + topics (`spec.parsed`, `drift.detect`, `docset.build`, `docset.publish`), Key Vault, Blob Storage account with immutable-prefix policy, Front Door CDN
- [X] T013a [P] Configure PostgreSQL Flexible Server with zone redundancy and geo-redundant backup in `infra/modules/postgres/main.tf` (RPO ≤ 1 h requires PITR + 7-day retention with cross-region backup; this is the foundation for SC-007)
- [X] T013b [P] Configure Service Bus namespace with geo-disaster recovery pairing in `infra/modules/servicebus/main.tf`
- [X] T013c [P] Configure Front Door origin failover in `infra/modules/frontdoor/main.tf` with health probe on `/actuator/health/liveness`; secondary origin in paired region
- [X] T013d [P] Schedule automated backup-verification job in `infra/modules/postgres/verify-backup.tf`
- [X] T014 [P] Configure Flyway baseline migration `backend/src/main/resources/db/migration/V001__init.sql` creating the `pgcrypto`, `pgvector` extensions and the schema: `tenants`, `users`, `projects`, `project_memberships`, `api_specs`, `endpoints`, `schemas`
- [X] T015 [P] Add Flyway migration `backend/src/main/resources/db/migration/V002__docset_drift.sql` creating: `doc_sets`, `drift_reports`, `drift_items`, `notification_channels`, `try_it_allowlist`, `try_it_secrets`, `audit_entries`
- [X] T016 [P] Add Flyway migration `backend/src/main/resources/db/migration/V003__rls_policies.sql` creating the row-level security policy template and `app.current_tenant` session-variable convention on every tenant-scoped table (per `data-model.md` §RLS)
- [X] T017 [P] Add Flyway migration `backend/src/main/resources/db/migration/V004__vector_embeddings.sql` creating: `example_embeddings`, `description_embeddings` with `vector(1536)` columns
- [X] T018 Implement OIDC/OAuth2 configuration in `backend/src/main/java/com/docsynth/infrastructure/security/OidcConfig.java` (issuer URI, JWKS, JWT decoder) per FR-010
- [X] T019 [P] Implement tenant context resolver in `backend/src/main/java/com/docsynth/infrastructure/security/TenantContextResolver.java` extracting `tenant_id` from the validated JWT, throwing on missing/invalid (FR-012 fail-closed)
- [X] T020 [P] Implement RBAC filter in `backend/src/main/java/com/docsynth/infrastructure/security/ProjectRbacFilter.java` enforcing Owner/Editor/Viewer roles (FR-010)
- [X] T021 Implement secret store client in `backend/src/main/java/com/docsynth/infrastructure/security/KeyVaultSecretStore.java` (FR-011: read-only from Key Vault, never log, never return)
- [X] T022 [P] Configure structured JSON logging in `backend/src/main/resources/logback-spring.xml` using logback-ecs-encoder with correlation/MDC fields (FR-013)
- [X] T023 [P] Configure Micrometer + OpenTelemetry in `backend/src/main/java/com/docsynth/config/TracingConfig.java` exporting OTLP to Azure Monitor, exposing `/actuator/prometheus` (FR-013)
- [X] T023a [P] Initialize OpenTelemetry SDK in `ai-sidecar/src/docsynth_ai/observability/tracing.py` with OTLP gRPC exporter; instrument gRPC server handlers; propagate W3C tracecontext across Java↔Python
- [X] T023b [P] Configure structured JSON logging in `ai-sidecar/src/docsynth_ai/observability/tracing.py` using `python-json-logger`; emit `correlation_id`, `tenant_id`, `service=ai-sidecar`
- [X] T023c [P] Emit RED metrics from `ai-sidecar/src/docsynth_ai/observability/tracing.py`: per-RPC rate, errors, duration histograms; expose via OTLP
- [X] T024 Implement transactional outbox in `backend/src/main/java/com/docsynth/infrastructure/messaging/Outbox.java` and outbox-relay worker publishing to Service Bus topics
- [X] T025 [P] Implement audit entry writer in `backend/src/main/java/com/docsynth/infrastructure/audit/JdbcAuditEmitter.java` (append-only, no UPDATE/DELETE, includes correlation_id; FR-009)
- [X] T025a [P] Define canonical `AuditEvent` schema in `backend/src/main/java/com/docsynth/domain/audit/AuditEvent.java` with required fields
- [X] T025b [P] Define `AuditEmitter` port in `backend/src/main/java/com/docsynth/application/audit/AuditEmitter.java` with single `emit(AuditEventEnvelope)` method
- [X] T025c [P] ArchUnit test enforcing that every `*UseCase` class declares constructor parameter of type `AuditEmitter`
- [X] T026 [P] Implement `Tenant`, `User`, `Project`, `ProjectMembership` JPA entities in `backend/src/main/java/com/docsynth/domain/{tenant,user,project}/` (foundational for all stories)
- [X] T027 [P] Implement corresponding Spring Data JPA repositories with RLS-aware entity manager in `backend/src/main/java/com/docsynth/infrastructure/persistence/`
- [X] T028 [P] Implement global exception handler returning RFC 9457 Problem Details in `backend/src/main/java/com/docsynth/interfaces/api/ProblemDetailsHandler.java`
- [X] T029 [P] Define gRPC service contracts in `contracts/proto/ai_orchestration.proto`, `contracts/proto/ingestion.proto`, `contracts/proto/drift.proto` per `contracts/README.md` and generate Java + Python stubs via `buf generate`
- [X] T030 [P] Implement gRPC client (Java) configuration in `backend/src/main/java/com/docsynth/infrastructure/grpc/AiSidecarClientConfig.java` with tenant-assertion JWT signing
- [X] T031 [P] Implement gRPC server (Python) skeleton in `ai-sidecar/src/docsynth_ai/server.py` with tenant-assertion JWT validation (RS256, JWKS, reject `UNAUTHENTICATED` if missing/invalid/expired; reject `PERMISSION_DENIED` if JWT tenant_id mismatches in-message)
- [X] T032 [P] Implement MCP server (Java) in `backend/src/main/java/com/docsynth/infrastructure/ai/McpServer.java` exposing read-only domain tools (per constitution: MCP is for tool exposure, not main contract)
- [X] T033 [P] Create base Vue 3 operator shell in `frontend/src/operator/` with: Pinia stores, Vue Router with OIDC callback handler, REST API client (`src/operator/api/client.ts`)
- [X] T034 [P] Create base Vue 3 docs SSG shell in `frontend/src/docs/` with: endpoint reference page component, code-example component, Try It island placeholder, manifest-driven version selector
- [X] T035 [P] Create shared TypeScript types in `frontend/src/shared/types/` for `ApiSpec`, `Endpoint`, `DocSet`, `DriftReport`, `DriftItem` matching `data-model.md` shapes

**Checkpoint**: Foundation ready — user story implementation can now begin in parallel.

---

## Phase 3: User Story 1 — OpenAPI Spec Ingestion (Priority: P1) 🎯 MVP

**Goal**: Engineers submit OpenAPI 3.x specs via URL, file upload, or GitHub repo; system parses and stores endpoints/schemas and makes them available.

**Independent Test**: Submit a public OpenAPI 3.x spec URL; receive a parsed, structured representation of all endpoints and their schemas within 30 seconds (SC-001).

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T036 [P] [US1] Contract test: `POST /api/v1/projects/{projectId}/specs` accepts URL source in `backend/src/test/java/com/docsynth/interfaces/api/SpecIngestionContractTest.java`
- [X] T037 [P] [US1] Integration test: 50-endpoint spec ingestion completes in ≤ 30 s in `backend/src/test/java/com/docsynth/application/ingestion/IngestionSlaIT.java`
- [X] T038 [P] [US1] Integration test: invalid spec URL returns clear error in `backend/src/test/java/com/docsynth/application/ingestion/InvalidSpecIT.java`
- [X] T039 [P] [US1] Integration test: GitHub repo source clones and detects spec file in `backend/src/test/java/com/docsynth/application/ingestion/GitHubIngestionIT.java`

### Implementation for User Story 1

- [X] T040 [P] [US1] Create `ApiSpec` JPA entity in `backend/src/main/java/com/docsynth/domain/ingestion/ApiSpec.java` per `data-model.md`
- [X] T041 [P] [US1] Create `Endpoint` JPA entity in `backend/src/main/java/com/docsynth/domain/ingestion/Endpoint.java` per `data-model.md`
- [X] T042 [P] [US1] Create `Schema` JPA entity in `backend/src/main/java/com/docsynth/domain/ingestion/Schema.java` per `data-model.md`
- [X] T043 [US1] Implement `SpecSource` value object (URL/file/repo) in `backend/src/main/java/com/docsynth/domain/ingestion/SpecSource.java`
- [X] T044 [US1] Implement `IngestSpecUseCase` in `backend/src/main/java/com/docsynth/application/ingestion/IngestSpecUseCase.java` orchestrating: download, parse, persist, publish `spec.parsed` event
- [X] T045 [US1] Implement `ParseSpecPort` (adapter) using `io.swagger.parser.v3:swagger-parser` in `backend/src/main/java/com/docsynth/infrastructure/parsing/SwaggerParserAdapter.java`
- [X] T046 [US1] Implement `SpecDownloaderPort` (URL fetch) in `backend/src/main/java/com/docsynth/infrastructure/ingestion/UrlSpecDownloader.java` with timeout + size cap
- [X] T047 [US1] Implement `GitHubClonePort` in `backend/src/main/java/com/docsynth/infrastructure/ingestion/GitHubCloneAdapter.java` (token-based clone + spec-file detection)
- [X] T048 [US1] Implement `SpecStoragePort` writing raw spec to Blob Storage in `backend/src/main/java/com/docsynth/infrastructure/ingestion/BlobSpecStorage.java`
- [X] T049 [US1] Implement `SpecController` in `backend/src/main/java/com/docsynth/interfaces/api/SpecController.java` (POST list, GET by id) with rate limit (10 req/min/user) and Problem Details errors
- [X] T050 [US1] Implement ingestion-event consumer in `backend/src/main/java/com/docsynth/application/ingestion/SpecParsedConsumer.java` for downstream triggers (e.g., drift re-eval)
- [X] T051 [US1] Implement `ApiSpec` repository in `backend/src/main/java/com/docsynth/infrastructure/persistence/ApiSpecRepository.java` with `findByProjectIdAndSpecSha256` (dedup)
- [X] T052 [US1] Wire audit entry for `ingest_spec` action in `IngestSpecUseCase` (FR-009)
- [X] T053 [P] [US1] Vue operator page: "Submit Spec" in `frontend/src/operator/pages/SpecSubmit.vue` with URL/file/repo form
- [X] T054 [P] [US1] Vue operator page: "Spec Detail" in `frontend/src/operator/pages/SpecDetail.vue` rendering parsed endpoints + schemas
- [X] T055 [P] [US1] Playwright E2E: submit a public spec and see parsed endpoints in `frontend/e2e/ingestion.spec.ts`

**Checkpoint**: US1 fully functional and independently testable. **MVP deliverable.**

---

## Phase 4: User Story 2 — Endpoint Reference & Example Generation (Priority: P1)

**Goal**: From a parsed spec, generate human-readable endpoint reference docs with descriptions, parameters, request/response schemas, and code examples in ≥ 1 language; docs are version-tagged.

**Independent Test**: After ingestion, navigate to a generated reference page for any endpoint with description, parameters, request/response schemas, and ≥ 1 working code example.

### Tests for User Story 2 ⚠️

- [X] T056 [P] [US2] Contract test: `POST /api/v1/projects/{projectId}/docsets` returns 202 + docSetId in `backend/src/test/java/com/docsynth/interfaces/api/DocSetGenerationContractTest.java`
- [X] T057 [P] [US2] Integration test: 50-endpoint docset is browsable within 60 s in `backend/src/test/java/com/docsynth/application/documentation/DocSetSlaIT.java`
- [X] T058 [P] [US2] Integration test: code example is syntactically valid (Python AST parse) in `backend/src/test/java/com/docsynth/application/documentation/CodeExampleValidityIT.java`
- [X] T059 [P] [US2] AI eval test: code-example relevance + faithfulness thresholds in `specs/evals/code_examples/test_relevance.py` and `test_faithfulness.py` (Constitution Principle III)
- [X] T060 [P] [US2] AI eval test: description-enhancement eval in `specs/evals/description_enhance/test_enhance.py`

### Implementation for User Story 2

- [X] T061 [P] [US2] Create `DocSet` JPA entity in `backend/src/main/java/com/docsynth/domain/documentation/DocSet.java` per `data-model.md`
- [X] T062 [US2] Implement `GenerateDocSetUseCase` in `backend/src/main/java/com/docsynth/application/documentation/GenerateDocSetUseCase.java` orchestrating: SSG build, manifest emission, blob upload
- [X] T063 [US2] Implement `DocSetBuilderPort` (adapter) that runs Vite SSG build in `backend/src/main/java/com/docsynth/infrastructure/documentation/ViteSsgAdapter.java`
- [X] T064 [P] [US2] Implement `CodeExampleGeneratorPort` calling the gRPC `DocGenerator` service in `backend/src/main/java/com/docsynth/infrastructure/documentation/CodeExampleClient.java`
- [X] T065 [US2] Implement `DescriptionEnhancerPort` calling the gRPC `DocGenerator.EnhanceDescription` in `backend/src/main/java/com/docsynth/infrastructure/documentation/DescriptionEnhancerClient.java`
- [X] T066 [P] [US2] Implement gRPC handler `DocGenerator` in `ai-sidecar/src/docsynth_ai/pipelines/code_examples.py` (LangChain code-gen pipeline)
- [X] T067 [P] [US2] Implement gRPC handler `DocGenerator.EnhanceDescription` in `ai-sidecar/src/docsynth_ai/pipelines/description_enhance.py`
- [X] T068 [P] [US2] Implement prompt-template registry in `ai-sidecar/src/docsynth_ai/prompts/` with versioned YAML files (reproducibility per AI eval gates)
- [X] T069 [US2] Implement `DocSetController` in `backend/src/main/java/com/docsynth/interfaces/api/DocSetController.java` (POST generate, GET list, GET by id)
- [X] T070 [US2] Implement manifest emitter in `backend/src/main/java/com/docsynth/infrastructure/documentation/ManifestEmitter.java` writing `index.json` per `research.md` §Storage layout
- [X] T071 [US2] Wire audit entry for `generate_docset` and `publish_docset` actions
- [X] T072 [P] [US2] Vue docs SSG page: endpoint reference in `frontend/src/docs/pages/EndpointReference.vue` (description, parameters, request/response schemas, code examples, version selector)
- [X] T073 [P] [US2] Vue docs SSG component: code-example block in `frontend/src/docs/components/CodeExample.vue` (syntax-highlighted, language switcher)
- [X] T074 [P] [US2] Vue docs SSG component: version dropdown in `frontend/src/docs/components/VersionSelector.vue` driven by `index.json` manifest
- [X] T075 [P] [US2] Playwright E2E: generated doc page renders with code example in `frontend/e2e/docs_render.spec.ts`

**Checkpoint**: US1 + US2 both work independently. MVP-Plus: ingested specs produce browsable, versioned doc pages.

---

## Phase 5: User Story 3 — Drift Detection & Alerts (Priority: P2)

**Goal**: On every push, compare the live spec against the last published docs; flag added/removed/changed items; send alerts to configured channels (Slack/email/CI).

**Independent Test**: After initial ingestion, submit a modified spec and receive a drift report within 2 minutes (SC-003).

### Tests for User Story 3 ⚠️

- [X] T076 [P] [US3] Contract test: `POST /api/v1/projects/{projectId}/drift` triggers comparison in `backend/src/test/java/com/docsynth/interfaces/api/DriftContractTest.java`
- [X] T077 [P] [US3] Integration test: drift report within 2 min for 50-endpoint spec in `backend/src/test/java/com/docsynth/application/drift/DriftSlaIT.java`
- [X] T078 [P] [US3] Integration test: added/removed/changed classification correct in `backend/src/test/java/com/docsynth/application/drift/DriftClassificationIT.java`
- [X] T078a [P] [US3] Unit test: description-only edits are `informational` (not breaking / non-breaking) in `backend/src/test/java/com/docsynth/infrastructure/drift/InformationalScopeTest.java`
- [X] T079 [P] [US3] Integration test: Slack notification dispatched on drift in `backend/src/test/java/com/docsynth/application/drift/NotificationIT.java`
- [X] T079a [P] [US3] Integration test: EmailNotifier dispatches drift report to test SMTP capture; no secrets in headers
- [X] T079b [P] [US3] Integration test: CiCheckNotifier returns a failing check status with drift report URL and severity counts
- [X] T079c [P] [US3] Integration test: NotificationChannel secret never appears in any log line (FR-011 cross-check) in `backend/src/test/java/com/docsynth/application/notification/NotificationSecretLeakIT.java`
- [X] T080 [P] [US3] Integration test: concurrent pushes serialize correctly (no double-report) in `backend/src/test/java/com/docsynth/application/drift/ConcurrentPushIT.java`

### Implementation for User Story 3

- [X] T081 [P] [US3] Create `DriftReport` JPA entity in `backend/src/main/java/com/docsynth/domain/drift/DriftReport.java` per `data-model.md`
- [X] T082 [P] [US3] Create `DriftItem` JPA entity in `backend/src/main/java/com/docsynth/domain/drift/DriftItem.java` per `data-model.md`
- [X] T083 [P] [US3] Create `NotificationChannel` JPA entity in `backend/src/main/java/com/docsynth/domain/drift/NotificationChannel.java` per `data-model.md`
- [X] T084 [US3] Implement `DetectDriftUseCase` in `backend/src/main/java/com/docsynth/application/drift/DetectDriftUseCase.java` orchestrating: baseline resolution, diff, classification, persistence, notification fan-out
- [X] T085 [US3] Implement `DiffSpecPort` using `org.openapitools.openapidiff:openapi-diff-core` in `backend/src/main/java/com/docsynth/infrastructure/drift/OpenApiDiffAdapter.java` with custom SPI rules for "informational" classification; SPI rules MUST operate only on the structural compatibility model
- [X] T086 [US3] Implement baseline-resolver `BaselineResolverPort` returning the latest published DocSet's source spec for the project in `backend/src/main/java/com/docsynth/infrastructure/drift/LatestPublishedBaseline.java`
- [X] T087 [US3] Implement `DriftController` in `backend/src/main/java/com/docsynth/interfaces/api/DriftController.java` (POST trigger, GET list, GET by id, filter by compatibility)
- [X] T088 [US3] Implement `NotificationController` in `backend/src/main/java/com/docsynth/interfaces/api/NotificationController.java` (CRUD channels)
- [X] T089 [P] [US3] Implement `SlackNotifier` in `backend/src/main/java/com/docsynth/infrastructure/notification/SlackNotifier.java` (injects secret from Key Vault, never logs)
- [X] T090 [P] [US3] Implement `EmailNotifier` in `backend/src/main/java/com/docsynth/infrastructure/notification/EmailNotifier.java`
- [X] T091 [P] [US3] Implement `CiCheckNotifier` in `backend/src/main/java/com/docsynth/infrastructure/notification/CiCheckNotifier.java` (returns failing check status)
- [X] T092 [US3] Implement `DriftDetectedConsumer` in `backend/src/main/java/com/docsynth/application/drift/DriftDetectedConsumer.java` subscribing to the outbox topic with idempotency key
- [X] T093 [US3] Implement webhook receiver `WebhookController` in `backend/src/main/java/com/docsynth/interfaces/api/WebhookController.java` for GitHub push events (signature verification, tenant resolution)
- [X] T094 [US3] Wire audit entry for `detect_drift` and `notify_drift` actions (FR-009, FR-005)
- [X] T095 [P] [US3] Vue operator page: "Drift Reports" in `frontend/src/operator/pages/DriftReports.vue` (list with filters)
- [X] T096 [P] [US3] Vue operator page: "Drift Detail" in `frontend/src/operator/pages/DriftDetail.vue` (added/removed/changed grouping, breaking vs non-breaking)
- [X] T097 [P] [US3] Vue operator page: "Notification Channels" in `frontend/src/operator/pages/Channels.vue` (CRUD)
- [X] T098 [P] [US3] Playwright E2E: trigger drift and see report in `frontend/e2e/drift.spec.ts`

**Checkpoint**: US1, US2, US3 all work independently. P2 complete: drift detection + alerts functional.

---

## Phase 6: User Story 4 — Interactive API Console (Priority: P3)

**Goal**: Browser-based "Try It" console that proxies requests to the target API through a tenant-scoped, allowlist-validated server-side proxy per `research.md §6`.

**Independent Test**: On any endpoint reference page, fill in example parameters, click "Try It", see a live response from the target API within 5 s (SC-004).

### Tests for User Story 4 ⚠️

- [X] T099 [P] [US4] Security test: SSRF — target host resolves to private IP is rejected in `backend/src/test/java/com/docsynth/infrastructure/proxy/SsrfGuardTest.java`
- [X] T100 [P] [US4] Security test: outbound redirects disabled in `backend/src/test/java/com/docsynth/infrastructure/proxy/RedirectDisabledTest.java`
- [X] T101 [P] [US4] Security test: secret never appears in response body or log in `backend/src/test/java/com/docsynth/infrastructure/proxy/SecretLeakTest.java`
- [X] T102 [P] [US4] Security test: rate limit returns 429 + Retry-After in `backend/src/test/java/com/docsynth/infrastructure/proxy/RateLimitTest.java`
- [X] T103 [P] [US4] Security test: request token rejected on cross-tenant replay in `backend/src/test/java/com/docsynth/infrastructure/proxy/TokenReplayTest.java`
- [X] T104 [P] [US4] Functional test: live response within 5 s in `backend/src/test/java/com/docsynth/infrastructure/proxy/TryItLatencyIT.java`
- [X] T105 [P] [US4] Functional test: target API error surfaced with status + body in `backend/src/test/java/com/docsynth/infrastructure/proxy/ErrorPassthroughIT.java`
- [X] T105a [P] [US4] Integration test: correlation_id from Java REST call is propagated to Python sidecar logs and back; assert presence via TraceparentCarrier in `backend/src/test/java/com/docsynth/infrastructure/proxy/CorrelationPropagationIT.java`

### Implementation for User Story 4

- [X] T106 [P] [US4] Create `TryItAllowlistEntry` JPA entity in `backend/src/main/java/com/docsynth/domain/proxy/TryItAllowlistEntry.java` per `data-model.md`
- [X] T107 [P] [US4] Create `TryItSecret` reference entity in `backend/src/main/java/com/docsynth/domain/proxy/TryItSecret.java` (Key Vault reference only)
- [X] T108 [US4] Implement `TryItProxyUseCase` in `backend/src/main/java/com/docsynth/application/proxy/TryItProxyUseCase.java`
- [X] T109 [US4] Implement `TargetAllowlistPolicy` in `backend/src/main/java/com/docsynth/application/proxy/TargetAllowlistPolicy.java` (resolves + validates target against `try_it_allowlist`)
- [X] T110 [US4] Implement `SsrfGuard` in `backend/src/main/java/com/docsynth/infrastructure/proxy/SsrfGuard.java` (IP-class check, scheme allowlist, DNS-rebinding mitigation, redirect disable)
- [X] T111 [US4] Implement `ProxyHttpClient` in `backend/src/main/java/com/docsynth/infrastructure/proxy/ProxyHttpClient.java` (request/response caps, timeouts, header sanitization)
- [X] T112 [US4] Implement `ProxyTokenIssuer` issuing session-bound HMAC tokens in `backend/src/main/java/com/docsynth/infrastructure/proxy/ProxyTokenIssuer.java`
- [X] T113 [US4] Implement per-tenant + per-user rate limiter (token bucket, in-memory v1) in `backend/src/main/java/com/docsynth/infrastructure/proxy/InMemoryRateLimiter.java`
- [X] T114 [US4] Implement `ProxyController` in `backend/src/main/java/com/docsynth/interfaces/api/ProxyController.java` (POST /api/v1/proxy/try)
- [X] T115 [US4] Implement `TryItAllowlistController` in `backend/src/main/java/com/docsynth/interfaces/api/TryItAllowlistController.java` (CRUD allowlist hosts)
- [X] T116 [US4] Implement `TryItSecretController` in `backend/src/main/java/com/docsynth/interfaces/api/TryItSecretController.java` (reference CRUD; value never returned)
- [X] T116a [US4] Implement secret rotation endpoint in `backend/src/main/java/com/docsynth/interfaces/api/TryItSecretController.java` (POST /api/v1/projects/{id}/tryit/secrets/{secretId}/rotate)
- [X] T117 [US4] Wire audit entry for `tryit_proxy_call` with `(tenant, user, targetHost, method, path, status, bytes, duration, requestId)` — no headers, no body, no auth (per `research.md §4`)
- [X] T118 [P] [US4] Vue docs SSG component: Try It island in `frontend/src/docs/components/TryItConsole.vue` (parameters form, request token fetch, response display)
- [X] T119 [P] [US4] Vue operator page: "Try It Settings" in `frontend/src/operator/pages/TryItSettings.vue` (allowlist CRUD, secret references)
- [X] T120 [P] [US4] Playwright E2E: try-it round-trip with a stub API in `frontend/e2e/tryit.spec.ts`

**Checkpoint**: US1, US2, US3, US4 all work independently. P3-half complete: Try It console live.

---

## Phase 7: User Story 5 — Versioned Publishing (Priority: P3)

**Goal**: Publish versioned doc sets at stable URLs; manage active/deprecated/archived lifecycle per FR-014.

**Independent Test**: After publishing, access `https://docs.example.com/v1/` and `https://docs.example.com/v2/` and see different content.

### Tests for User Story 5 ⚠️

- [X] T121 [P] [US5] Contract test: `PATCH /api/v1/projects/{projectId}/docsets/{docSetId}/state` enforces FR-014 transitions in `backend/src/test/java/com/docsynth/interfaces/api/DocSetStateContractTest.java`
- [X] T122 [P] [US5] Integration test: deprecated DocSet displays banner; archived returns 410 after 90 d in `backend/src/test/java/com/docsynth/infrastructure/publishing/PublishingLifecycleIT.java`
- [X] T123 [P] [US5] Integration test: publishing v2 auto-supersedes v1 active in `backend/src/test/java/com/docsynth/application/documentation/SupersedeIT.java`
- [X] T124 [P] [US5] Integration test: `archived → active` transition rejected in `backend/src/test/java/com/docsynth/application/documentation/ArchivedReviveIT.java`
- [X] T125 [P] [US5] Integration test: 90-day gone worker flips `gone_at` and serves 410 in `backend/src/test/java/com/docsynth/application/documentation/GoneWorkerIT.java`

### Implementation for User Story 5

- [X] T126 [US5] Implement `DocSetStateMachine` in `backend/src/main/java/com/docsynth/application/documentation/DocSetStateMachine.java` enforcing FR-014 transition rules; rejects `archived → active`; rejects `deprecated → archived` if < 90 d since `deprecated_at`
- [X] T127 [US5] Implement `PublishDocSetUseCase` in `backend/src/main/java/com/docsynth/application/documentation/PublishDocSetUseCase.java` (auto-supersede previous active; emit CDN cache invalidation message)
- [X] T128 [US5] Implement `TransitionDocSetStateUseCase` in `backend/src/main/java/com/docsynth/application/documentation/TransitionDocSetStateUseCase.java`
- [X] T129 [US5] Implement `DocSetStateController` (PATCH) in `backend/src/main/java/com/docsynth/interfaces/api/DocSetStateController.java`
- [X] T130 [US5] Implement deprecation-banner upload in `backend/src/main/java/com/docsynth/infrastructure/publishing/DeprecationBannerEmitter.java` (uploads banner partial; updates manifest)
- [X] T131 [US5] Implement 410 Gone worker in `backend/src/main/java/com/docsynth/application/documentation/GoneWorker.java` (scheduled daily; flips `gone_at` for archived DocSets > 90 d; uploads `410.html`; updates manifest)
- [X] T132 [US5] Implement Front Door cache invalidation adapter in `backend/src/main/java/com/docsynth/infrastructure/publishing/FrontDoorCacheInvalidator.java` (path-prefix purge on supersede/archive/gone)
- [X] T133 [US5] Wire audit entry for all DocSet state transitions
- [X] T134 [P] [US5] Vue docs SSG component: deprecation banner in `frontend/src/docs/components/DeprecationBanner.vue` (reads from manifest; auto-hides on replacement click)
- [X] T135 [P] [US5] Vue operator page: "DocSet Management" in `frontend/src/operator/pages/DocSetManagement.vue` (state transition UI with role gating)
- [X] T136 [P] [US5] Vue docs SSG page: 410.html stub in `frontend/src/docs/pages/410.html` (link to current version)

**Checkpoint**: All 5 user stories work independently. P3 complete: full versioned publishing lifecycle.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories; production-readiness gates per Constitution Principles III and IV.

- [ ] T137 [P] Run quickstart.md end-to-end validation and capture evidence in `specs/001-docsynth-core/quickstart-validation.md`
- [ ] T138 [P] Load test: 100 projects × 500 endpoints (SC-006) in `backend/src/test/java/com/docsynth/load/ScaleIT.java` using k6 or Gatling
- [ ] T139 [P] Load test: Try It proxy at 100 RPS sustained with SSRF/rate-limit protections active in `backend/src/test/java/com/docsynth/load/ProxyLoadIT.java`
- [ ] T140 [P] Security audit: OWASP top 10 review, dependency vulnerability scan (Trivy), secret-scan (TruffleHog) in `.github/workflows/security.yml`
- [ ] T141 [P] Disaster-recovery drill: restore from backup within RPO 1 h / RTO 4 h, document runbook in `infra/runbooks/dr.md` (SC-007)
- [ ] T142 [P] AI eval thresholds locked in CI: `specs/evals/thresholds.yml` enforced as build gate; no deploy if any threshold regresses (Constitution Principle III)
- [ ] T143 [P] ArchUnit architecture rules enforced in CI: failure fails the build (Constitution Principle II)
- [ ] T144 [P] Observability dashboards published: RED/USE for ingest, generate, drift, publish, proxy (FR-013) in `infra/observability/dashboards/`
- [ ] T145 [P] Runbooks for on-call: ingest failure, drift backlog, proxy SSRF attempt, OIDC token expiry, Service Bus poison message in `infra/runbooks/`
- [ ] T146 [P] Documentation: operator user guide, API reference (auto-generated from `contracts/openapi/operator-api.yaml`), architecture decision records in `docs/adr/`
- [ ] T147 [P] Code cleanup, dead-code removal, final Spotless + ruff + ESLint + mypy passes across all modules

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — **BLOCKS all user stories**
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order: US1 → US2 → US3 → US4 → US5
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1, Ingestion)**: Can start after Foundational — no dependencies on other stories
- **US2 (P1, Doc Generation)**: Can start after Foundational — reads from US1's `ApiSpec` rows but is independently testable with seeded fixtures
- **US3 (P2, Drift)**: Can start after Foundational — depends on US1 + US2 having produced a published DocSet baseline; independently testable with seeded baselines
- **US4 (P3, Try It)**: Can start after Foundational — depends on US2's docs page; independently testable with a stub target API
- **US5 (P3, Versioned Publishing)**: Can start after Foundational — depends on US2's DocSet production; independently testable with seeded DocSets

### Within Each User Story

- Tests (Constitution Principle III) MUST be written and FAIL before implementation
- Models (JPA entities) before services (use cases)
- Services before endpoints (controllers)
- Core implementation before integration (consumers, webhooks)
- Audit entry wiring happens at the use-case layer
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel (T002..T012)
- All Foundational tasks marked [P] can run in parallel within Phase 2
- Once Foundational phase completes, US1 and US2 can start in parallel; US3 waits for US1+US2 to seed at least one published DocSet for the SLA test, but the use-case code can be written in parallel
- US4 and US5 can be developed in parallel with each other and with US3 once Foundational is done
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on by different team members simultaneously

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (must FAIL first):
Task: "Contract test: POST /api/v1/projects/{projectId}/specs in backend/src/test/.../SpecIngestionContractTest.java"
Task: "Integration test: 50-endpoint spec ingestion ≤ 30 s in backend/src/test/.../IngestionSlaIT.java"
Task: "Integration test: invalid spec URL returns clear error in backend/src/test/.../InvalidSpecIT.java"
Task: "Integration test: GitHub repo source in backend/src/test/.../GitHubIngestionIT.java"

# Launch all models for User Story 1 together (parallel — different files):
Task: "Create ApiSpec JPA entity in backend/src/main/java/com/docsynth/domain/ingestion/ApiSpec.java"
Task: "Create Endpoint JPA entity in backend/src/main/java/com/docsynth/domain/ingestion/Endpoint.java"
Task: "Create Schema JPA entity in backend/src/main/java/com/docsynth/domain/ingestion/Schema.java"

# Launch all Vue pages for User Story 1 together (parallel — different files):
Task: "Vue operator page: SpecSubmit.vue in frontend/src/operator/pages/SpecSubmit.vue"
Task: "Vue operator page: SpecDetail.vue in frontend/src/operator/pages/SpecDetail.vue"
Task: "Playwright E2E: ingestion.spec.ts in frontend/e2e/ingestion.spec.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001..T012)
2. Complete Phase 2: Foundational (T013..T035) — **CRITICAL: blocks all stories**
3. Complete Phase 3: User Story 1 (T036..T055)
4. **STOP and VALIDATE**: ingest a public OpenAPI 3.x spec URL, see parsed endpoints within 30 s
5. Deploy/demo if ready — this is the smallest viable DocSynth

### Incremental Delivery

1. Setup + Foundational → Foundation ready (T001..T035)
2. **US1** → Ingest specs → Deploy/Demo (MVP!) (T036..T055)
3. **US2** → Generate docs + code examples → Deploy/Demo (T056..T075)
4. **US3** → Drift detection + alerts → Deploy/Demo (T076..T098)
5. **US4** → Try It console → Deploy/Demo (T099..T120)
6. **US5** → Versioned publishing lifecycle → Deploy/Demo (T121..T136)
7. Each story adds value without breaking previous stories
8. Polish: scale, security, observability, runbooks, docs (T137..T147)

### Parallel Team Strategy

With multiple developers (4-5 recommended):

1. Team completes Setup + Foundational together (1-2 weeks)
2. Once Foundational is done:
   - Developer A: US1 (Ingestion)
   - Developer B: US2 (Doc Generation) — overlaps A; both unblock US3
   - Developer C: US4 (Try It proxy) — independent, can start immediately
   - Developer D: US5 (Versioned publishing) — independent, can start immediately
   - Once US1 + US2 ship a published DocSet: Developer E picks up US3 (Drift)
3. Stories complete and integrate independently; AI sidecar work (T066..T068, plus T067) can be picked up by a Python-focused developer in parallel

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to user story for spec traceability
- Each user story is independently completable and testable
- Tests are written and verified to fail before implementation (Constitution Principle III, TDD)
- Audit entry wiring is non-negotiable (FR-009); missing it is a review blocker
- RLS policy enforcement (FR-012) is verified by integration tests, not unit tests
- Constitution Check (Principles I-V) re-evaluated in every PR
- Stop at any checkpoint to validate the story independently before moving on
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence
