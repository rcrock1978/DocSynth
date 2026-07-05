# Feature Specification: DocSynth Core — AI API Documentation Generator

**Feature Branch**: `001-docsynth-core`

**Created**: 2026-07-01

**Status**: Draft

**Input**: User description: "Build DocSynth — an AI-powered API documentation generator that stays in sync with the codebase."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — OpenAPI Spec Ingestion (Priority: P1)

A backend engineer points DocSynth at a GitHub repository. DocSynth introspects the OpenAPI spec and source code, parsing endpoints, schemas, and operation details. The engineer sees the parsed spec immediately without writing any extraction scripts.

**Why this priority**: Every downstream feature depends on having parsed, accurate API specs. Without ingestion, nothing else works.

**Independent Test**: A user can submit a public OpenAPI spec URL and receive a parsed, structured representation of all endpoints and their schemas within 30 seconds.

**Acceptance Scenarios**:

1. **Given** a valid public OpenAPI 3.x spec URL, **When** the user submits it via the web UI or CLI, **Then** the system parses all endpoints, request/response schemas, and displays them in a structured view within 30 seconds.
2. **Given** a private GitHub repository URL with access token, **When** the user submits it, **Then** the system clones/introspects the repo, detects the OpenAPI spec file, and parses it with the same result.
3. **Given** an invalid or malformed spec URL, **When** the user submits it, **Then** the system returns a clear error message describing what failed and where.

---

### User Story 2 — Endpoint Reference & Example Generation (Priority: P1)

After ingesting a spec, the engineer wants human-readable API reference docs with auto-generated code examples. The system generates both, organized by endpoint and tagged by version.

**Why this priority**: Readable, example-rich docs are the core value proposition — this is the primary output users pay for.

**Independent Test**: After spec ingestion, the user can navigate a generated reference page for any endpoint that includes description, parameters, request/response schemas, and at least one working code example.

**Acceptance Scenarios**:

1. **Given** a parsed OpenAPI spec, **When** the user requests endpoint reference docs, **Then** each endpoint shows name, method, path, description, query/path/header parameters, request body schema, and response schemas.
2. **Given** a parsed endpoint, **When** the user requests code examples, **Then** the system generates syntactically valid snippets in at least one target language (e.g., cURL, Python, Java).
3. **Given** the system generates docs, **When** a new spec version is ingested, **Then** the docs are version-tagged and the previous version remains accessible.

---

### User Story 3 — Drift Detection & Alerts (Priority: P2)

The engineer sets up a webhook or CI integration. On every push, DocSynth compares the live spec against the last published docs and flags any discrepancies — added, removed, or changed endpoints, modified schemas, or altered parameters.

**Why this priority**: Drift detection is the key differentiator that prevents docs from silently going stale.

**Independent Test**: After initial ingestion, the user submits a modified spec and receives a drift report highlighting what changed within 2 minutes.

**Acceptance Scenarios**:

1. **Given** an already-published doc set, **When** a new spec version is ingested with added endpoints, **Then** the drift report identifies the new endpoints as "added."
2. **Given** an already-published doc set, **When** a new spec version removes an endpoint or changes a schema field, **Then** the drift report marks those as "removed" or "changed."
3. **Given** a drift report is generated, **When** configured notifications are enabled, **Then** the system sends an alert (e.g., Slack message, email, or CI check failure).

---

### User Story 4 — Interactive API Console (Priority: P3)

A consumer visiting the docs can try any endpoint directly from the browser — fill in parameters, send a live request to the underlying API, and see the real response.

**Why this priority**: This accelerates time-to-first-call and reduces context switching for API consumers.

**Independent Test**: On any endpoint reference page, the user can fill in example parameters, click "Try It," and see a live response from the target API.

**Acceptance Scenarios**:

1. **Given** an endpoint with documented parameters, **When** the user fills in values and clicks "Try It," **Then** the system proxies the request to the actual API and displays the response inline.
2. **Given** the target API returns an error, **When** the user tries the endpoint, **Then** the system displays the error response with status code and body.
3. **Given** the endpoint requires authentication headers, **When** the user provides a valid API key in the console, **Then** the request is authenticated and the response is returned.

---

### User Story 5 — Versioned Publishing (Priority: P3)

The docs owner publishes a versioned doc set that is publicly accessible at a stable URL. Each release creates a snapshot; the team can manage which versions are active, deprecated, or archived.

**Why this priority**: Versioned publishing enables teams to maintain docs for multiple API versions simultaneously, critical for non-breaking-release workflows.

**Independent Test**: After publishing, the user can access `https://docs.example.com/v1/` and `https://docs.example.com/v2/` and see different content.

**Acceptance Scenarios**:

1. **Given** published docs for version 1.0 and 2.0, **When** a user navigates to `/v1/`, **Then** they see only the 1.0 endpoints and schemas.
2. **Given** an active published version, **When** the team marks it as deprecated, **Then** the doc page displays a deprecation banner but remains accessible.
3. **Given** a published doc set, **When** the team archives it, **Then** the docs are removed from the public listing but remain accessible via direct link for 90 days.

### Edge Cases

- What happens when the OpenAPI spec is huge (500+ endpoints)?
- How does the system handle specs without any operation IDs?
- What happens when the CI integration receives concurrent pushes to the same repo?
- How does drift detection handle non-breaking changes (e.g., adding an optional field)?
- What happens when the interactive console targets a rate-limited or down API?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept OpenAPI 3.x specs from a URL, file upload, or GitHub repository URL with access token.
- **FR-002**: System MUST parse and store all endpoints, schemas, parameters, and security definitions from the ingested spec.
- **FR-003**: System MUST generate human-readable endpoint reference docs with descriptions, parameters, request/response schemas, and code examples.
- **FR-004**: System MUST detect drift between an ingested spec and the previously published docs, identifying added, removed, and changed items.
- **FR-005**: System MUST notify configured channels (Slack, email, CI workflow) when drift is detected.
- **FR-006**: System MUST provide an interactive browser-based API console that proxies requests to the target API.
- **FR-007**: System MUST support versioned publishing with active, deprecated, and archived states.
- **FR-008**: System MUST isolate data per tenant so no tenant can access another tenant's specs or docs.
- **FR-009**: System MUST log all operations with enough context to diagnose issues and audit access.
- **FR-010**: System MUST authenticate users via OIDC/OAuth2 and authorize via role-based access control (RBAC); roles include at minimum Owner, Editor, and Viewer per project.
- **FR-011**: System MUST store secrets (GitHub tokens, API keys for the interactive console, notification credentials) in a managed secret store and MUST never log or expose them in API responses.
- **FR-012**: System MUST enforce tenant isolation at the data layer (row-level security) with a per-request tenant context propagated to every query; cross-tenant access MUST fail closed.
- **FR-013**: System MUST emit structured JSON logs with 90-day retention, RED/USE metrics (rate, errors, duration; utilization, saturation, errors), and distributed traces with correlation IDs propagated across all service boundaries.
- **FR-014**: DocSet state transitions MUST follow these rules:
  - `active → deprecated`: any project Owner or Editor, manual action, optional sunset date.
  - `active → active (supersede)`: publishing a new DocSet version auto-supersedes the previous active version.
  - `deprecated → active`: revert by Owner or Editor (corrects a mistake).
  - `deprecated → archived`: manual, minimum 90 days after deprecation.
  - `archived → active`: not allowed; require publishing a fresh DocSet.
  - All transitions MUST be recorded in AuditEntry; deprecated DocSets MUST display a deprecation banner; archived DocSets MUST return `410 Gone` after 90 days.

### Key Entities *(include if feature involves data)*

- **ApiSpec**: A parsed OpenAPI specification ingested from a URL, file, or repository. Contains endpoints, schemas, parameters, and security definitions.
- **DocSet**: A generated documentation set derived from an ApiSpec at a specific version. Contains rendered endpoint references, examples, and the interactive console configuration.
- **DriftReport**: A comparison result between two ApiSpec versions or between an ApiSpec and its published DocSet. Lists added, removed, and changed items.
- **Project**: A tenant-scoped container for ApiSpecs, DocSets, and configuration. Each project maps to one API product.
- **AuditEntry**: A record of every state-changing operation (ingest, generate, publish, detect drift) with timestamp, actor, tenant, and outcome.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A spec with 50 endpoints is ingested, parsed, and displayed within 30 seconds from submission.
- **SC-002**: Generated endpoint reference docs are available for browsing within 60 seconds of spec ingestion.
- **SC-003**: Drift detection completes and produces a report within 2 minutes of spec update.
- **SC-004**: The interactive console returns a live API response within 5 seconds of the user clicking "Try It."
- **SC-005**: 95% of users who complete ingestion and doc generation do so without needing support.
- **SC-006**: System supports up to 100 projects per tenant and 500 endpoints per project without degradation.
- **SC-007**: Service targets 99.5% monthly uptime with RPO ≤ 1 hour and RTO ≤ 4 hours for tenant-facing operations (ingest, generate, publish, drift).

## Assumptions

- Users have an existing OpenAPI 3.x specification for their API.
- Users have the authority to configure CI/webhook integrations on their repositories.
- Target APIs are reachable from the DocSynth infrastructure (no firewalled internal APIs for MVP).
- SDK code examples are initially generated for cURL, Python (requests), and Java (OkHttp); additional languages are added based on demand.
- Drift detection focuses on structural changes (endpoints, schemas, parameters) not semantic changes (e.g., changed business logic within an operation).
- Users access the system via modern web browsers (Chrome, Firefox, Safari, Edge — latest 2 major versions).

## Out of Scope

The following are explicitly **not** part of this feature and are deferred to future releases:

- **Non-OpenAPI spec support**: GraphQL, gRPC/protobuf, AsyncAPI, Postman collections.
- **Semantic diff**: detecting changes in business logic or operation behavior; only structural drift (endpoints, schemas, parameters) is detected.
- **Self-hosted / on-prem deployment**: DocSynth is delivered as a managed multi-tenant SaaS only.
- **Custom domain hosting** for published doc sets (e.g., `docs.customer.com`); public URLs use a DocSynth-owned domain.
- **Enterprise SSO / SCIM provisioning**: per-tenant SSO (SAML/Okta/Azure AD) and SCIM user lifecycle are deferred.
- **AI-generated changelogs** and natural-language release notes from drift reports.

## Clarifications

### Session 2026-07-03

- Q: What availability target should the service commit to? → A: Target SLA — 99.5% monthly uptime, RPO 1h, RTO 4h (tenant-facing operations only).
- Q: What authentication and tenant-isolation model applies? → A: OIDC/OAuth2 + RBAC, secrets in managed vault, row-level security with per-request tenant context.
- Q: What observability depth is required? → A: Structured JSON logs (90d retention), RED/USE metrics, distributed tracing with correlation IDs across all services.
- Q: Should the spec include an explicit Out of Scope section? → A: Yes — added explicit Out of Scope section covering non-OpenAPI specs, semantic diff, on-prem, custom domains, enterprise SSO/SCIM, and AI changelogs.
- Q: How should DocSet lifecycle state transitions be governed? → A: Explicit rules — Owner/Editor can deprecate (with optional sunset) or revert; publishing a new version auto-supersedes; deprecation must precede archiving by ≥90 days; archived is terminal; all transitions audited; deprecated show banner; archived return 410 Gone after 90 days.
